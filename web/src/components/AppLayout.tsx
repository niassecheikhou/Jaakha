import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import footerLogoJaankha from '../assets/logofond.png'
import logoJaankha from '../assets/logojaankha.png'
import { useAuth } from '../hooks/use-auth'
import { getApiError, notificationsApi } from '../services/api'
import { isAdministrationRole } from '../utils/roles'
import { useToast } from './toast-context'

export function AppLayout() {
  const { user, logout } = useAuth()
  const toast = useToast()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  function closeMobileMenu() {
    setMobileMenuOpen(false)
  }

  function handleLogout() {
    closeMobileMenu()
    logout()
    toast.success('Deconnexion reussie', 'Ta session a ete fermee.')
  }

  return (
    <div className="shell">
      <header className={`topbar ${mobileMenuOpen ? 'mobile-menu-open' : ''}`}>
        <NavLink className="brand" to="/" aria-label="Accueil Jaankha" onClick={closeMobileMenu}>
          <img src={logoJaankha} alt="Jaankha" />
        </NavLink>
        <button
          aria-controls="primary-navigation"
          aria-expanded={mobileMenuOpen}
          aria-label={mobileMenuOpen ? 'Fermer le menu' : 'Ouvrir le menu'}
          className="mobile-menu-toggle"
          type="button"
          onClick={() => setMobileMenuOpen((current) => !current)}
        >
          {mobileMenuOpen ? <XIcon /> : <MenuIcon />}
        </button>
        <nav className="nav" id="primary-navigation">
          <NavLink to="/about" onClick={closeMobileMenu}>A propos</NavLink>
          <NavLink to="/services" onClick={closeMobileMenu}>Nos services</NavLink>
          <NavLink to="/providers" onClick={closeMobileMenu}>Prestataires</NavLink>
          <NavLink to="/contact" onClick={closeMobileMenu}>Contact</NavLink>
          {user?.role === 'EMPLOYEUR' && <NavLink to="/employer" onClick={closeMobileMenu}>Mes reservations</NavLink>}
          {user?.role === 'PRESTATAIRE' && <NavLink to="/provider" onClick={closeMobileMenu}>Espace prestataire</NavLink>}
          {isAdministrationRole(user?.role) && <NavLink to="/admin" onClick={closeMobileMenu}>Admin</NavLink>}
        </nav>
        <div className="session">
          {user ? (
            <>
              <TopbarNotifications />
              <button className="button secondary" type="button" onClick={handleLogout}>
                Deconnexion
              </button>
            </>
          ) : (
            <>
              <NavLink className="button secondary" to="/login" onClick={closeMobileMenu}>
                Se connecter
              </NavLink>
              <NavLink className="button primary nav-cta" to="/register" onClick={closeMobileMenu}>
                Commencer gratuitement
                <span aria-hidden="true">-&gt;</span>
              </NavLink>
            </>
          )}
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="site-footer-inner">
          <div className="footer-brand-block">
            <div className="footer-logo">
              <img src={footerLogoJaankha} alt="Jaankha" />
            </div>
            <p>Votre partenaire pour des services a domicile fiables au Senegal.</p>
          </div>
          <nav className="footer-column" aria-label="Produits">
            <h2>Produits</h2>
            <NavLink to="/services">Nos services</NavLink>
            <NavLink to="/providers">Prestataires</NavLink>
            <NavLink to="/register">Inscription</NavLink>
            <NavLink to="/login">Connexion</NavLink>
          </nav>
          <nav className="footer-column" aria-label="Entreprise">
            <h2>Entreprise</h2>
            <NavLink to="/about">A propos</NavLink>
            <NavLink to="/services">Services</NavLink>
            <NavLink to="/providers">Partenaires</NavLink>
          </nav>
          <nav className="footer-column" aria-label="Support">
            <h2>Support</h2>
            <NavLink to="/providers">Centre d'aide</NavLink>
            <NavLink to="/contact">Contact</NavLink>
          </nav>
          <div className="footer-bottom">
            <span>© 2026 Jaankha. Tous droits reserves.</span>
            <div>
              <span>SN&nbsp; Senegal</span>
              <span>DKR&nbsp; Dakar</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}

function TopbarNotifications() {
  const [open, setOpen] = useState(false)
  const queryClient = useQueryClient()
  const toast = useToast()
  const notificationsQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: notificationsApi.list,
  })
  const readMutation = useMutation({
    mutationFn: notificationsApi.markRead,
    onSuccess: () => {
      toast.success('Notification mise a jour', 'La notification a ete marquee comme lue.')
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (caughtError) => toast.error('Notification non mise a jour', getApiError(caughtError).message),
  })
  const notifications = notificationsQuery.data ?? []
  const unreadNotifications = notifications.filter((notification) => !notification.read)
  const visibleNotifications = unreadNotifications.slice(0, 5)
  const notificationError = notificationsQuery.isError ? getApiError(notificationsQuery.error).message : null

  return (
    <div className="topbar-notifications">
      <button
        aria-expanded={open}
        aria-label="Afficher les notifications"
        className="notification-bell-button"
        type="button"
        onClick={() => setOpen((current) => !current)}
      >
        <BellIcon />
        {unreadNotifications.length > 0 && (
          <span className="notification-badge">{unreadNotifications.length > 9 ? '9+' : unreadNotifications.length}</span>
        )}
      </button>
      {open && (
        <div className="notification-popover">
          <div className="notification-popover-header">
            <strong>Notifications</strong>
            <span>{unreadNotifications.length} non lue{unreadNotifications.length > 1 ? 's' : ''}</span>
          </div>
          {notificationError && <p className="notification-empty">{notificationError}</p>}
          {!notificationError && visibleNotifications.length === 0 && (
            <p className="notification-empty">Aucune notification.</p>
          )}
          {visibleNotifications.length > 0 && (
            <div className="notification-menu-list">
              {visibleNotifications.map((notification) => (
                <article className="notification-menu-item" key={notification.id}>
                  <strong>{notification.title}</strong>
                  <p>{notification.message}</p>
                  <button
                    className="link-button"
                    disabled={readMutation.isPending}
                    type="button"
                    onClick={() => readMutation.mutate(notification.id)}
                  >
                    Marquer comme lue
                  </button>
                </article>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function MenuIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M4 7h16M4 12h16M4 17h16" />
    </svg>
  )
}

function XIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  )
}

function BellIcon() {
  return (
    <svg aria-hidden="true" fill="none" height="22" viewBox="0 0 24 24" width="22">
      <path
        d="M18 8.8a6 6 0 0 0-12 0c0 7-3 7.5-3 7.5h18s-3-.5-3-7.5Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
      <path
        d="M14 20a2.3 2.3 0 0 1-4 0"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </svg>
  )
}
