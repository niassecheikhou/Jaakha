import { useCallback, useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

export type ActionsMenuItem = {
  id: string
  label: string
  icon?: ReactNode
  // Couleur de l'action : neutre par defaut.
  tone?: 'view' | 'approve' | 'reject' | 'archive'
  // Texte secondaire sous le libelle (ex. "Statut : En cours", "Apres acceptation").
  hint?: string
  // Action momentanement bloquee (requete en cours).
  disabled?: boolean
  // Etape pas encore possible : affichee grisee pour montrer le parcours, non cliquable.
  unavailable?: boolean
  onSelect: () => void
}

type ActionsMenuProps = {
  // Nom de l'element concerne, pour les lecteurs d'ecran ("Actions pour Awa Diop").
  subject: string
  items: ActionsMenuItem[]
}

const MENU_WIDTH = 270
const ITEM_HEIGHT = 42
const ITEM_WITH_HINT_HEIGHT = 56
const MENU_PADDING = 12
const VIEWPORT_MARGIN = 8

type MenuPosition = { top: number; left: number; openUp: boolean }

// Place le menu sous le bouton, aligne a droite ; au-dessus s'il n'y a pas la place en bas.
function menuPosition(button: HTMLElement, items: ActionsMenuItem[]): MenuPosition {
  const rect = button.getBoundingClientRect()
  const menuHeight = items.reduce((height, item) => height + (item.hint ? ITEM_WITH_HINT_HEIGHT : ITEM_HEIGHT), MENU_PADDING)
  const openUp = rect.bottom + menuHeight + VIEWPORT_MARGIN > window.innerHeight && rect.top > menuHeight
  const left = Math.min(
    Math.max(VIEWPORT_MARGIN, rect.right - MENU_WIDTH),
    window.innerWidth - MENU_WIDTH - VIEWPORT_MARGIN,
  )
  return { top: openUp ? rect.top - menuHeight - 6 : rect.bottom + 6, left, openUp }
}

function enabledItems(menu: HTMLElement | null) {
  return Array.from(menu?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]:not(:disabled)') ?? [])
}

// Menu d'actions "⋮" : regroupe les actions d'une ligne dans une liste deroulante.
// Rendu dans document.body (portail) pour ne pas etre coupe par le defilement des tableaux.
export function ActionsMenu({ subject, items }: ActionsMenuProps) {
  const [position, setPosition] = useState<MenuPosition | null>(null)
  const open = position !== null
  const buttonRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const menuId = useId()

  const close = useCallback((restoreFocus = true) => {
    setPosition(null)
    if (restoreFocus) {
      buttonRef.current?.focus()
    }
  }, [])

  // Fermeture : clic exterieur, defilement ou redimensionnement de la fenetre.
  useEffect(() => {
    if (!open) {
      return
    }
    function handlePointerDown(event: PointerEvent) {
      const target = event.target as Node
      if (!menuRef.current?.contains(target) && !buttonRef.current?.contains(target)) {
        close(false)
      }
    }
    function handleViewportChange() {
      close(false)
    }
    document.addEventListener('pointerdown', handlePointerDown)
    window.addEventListener('scroll', handleViewportChange, true)
    window.addEventListener('resize', handleViewportChange)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      window.removeEventListener('scroll', handleViewportChange, true)
      window.removeEventListener('resize', handleViewportChange)
    }
  }, [open, close])

  // A l'ouverture, le focus va sur la premiere action disponible.
  useEffect(() => {
    if (open) {
      enabledItems(menuRef.current)[0]?.focus()
    }
  }, [open])

  function toggle() {
    if (open) {
      close()
    } else if (buttonRef.current) {
      setPosition(menuPosition(buttonRef.current, items))
    }
  }

  function handleMenuKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const elements = enabledItems(menuRef.current)
    const index = elements.indexOf(document.activeElement as HTMLButtonElement)
    if (event.key === 'Escape') {
      event.preventDefault()
      close()
    } else if (event.key === 'ArrowDown') {
      event.preventDefault()
      elements[(index + 1) % elements.length]?.focus()
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      elements[(index - 1 + elements.length) % elements.length]?.focus()
    } else if (event.key === 'Home') {
      event.preventDefault()
      elements[0]?.focus()
    } else if (event.key === 'End') {
      event.preventDefault()
      elements[elements.length - 1]?.focus()
    } else if (event.key === 'Tab') {
      close(false)
    }
  }

  return (
    <>
      <button
        ref={buttonRef}
        aria-controls={open ? menuId : undefined}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label={`Actions pour ${subject}`}
        className={`icon-button actions-menu-trigger${open ? ' open' : ''}`}
        title="Actions"
        type="button"
        onClick={toggle}
      >
        <svg aria-hidden="true" viewBox="0 0 24 24">
          <circle cx="12" cy="5" r="1.6" />
          <circle cx="12" cy="12" r="1.6" />
          <circle cx="12" cy="19" r="1.6" />
        </svg>
      </button>
      {position && createPortal(
        <div
          ref={menuRef}
          aria-label={`Actions pour ${subject}`}
          className={`actions-menu${position.openUp ? ' open-up' : ''}`}
          id={menuId}
          role="menu"
          style={{ top: position.top, left: position.left, width: MENU_WIDTH }}
          onKeyDown={handleMenuKeyDown}
        >
          {items.map((item) => (
            <button
              aria-disabled={item.unavailable || undefined}
              className={`actions-menu-item${item.tone ? ` tone-${item.tone}` : ''}${item.unavailable ? ' unavailable' : ''}`}
              disabled={item.disabled || item.unavailable}
              key={item.id}
              role="menuitem"
              type="button"
              onClick={() => {
                close()
                item.onSelect()
              }}
            >
              {item.icon && <span className="actions-menu-icon">{item.icon}</span>}
              <span className="actions-menu-text">
                <span>{item.label}</span>
                {item.hint && <small>{item.hint}</small>}
              </span>
            </button>
          ))}
        </div>,
        document.body,
      )}
    </>
  )
}
