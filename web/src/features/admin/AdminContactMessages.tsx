import { useEffect, useState } from 'react'
import type { ContactMessage } from '../../types/api'
import { EyeIcon, XIcon } from './AdminIcons'

type ExportFormat = 'csv' | 'excel'
const MESSAGE_PREVIEW_LENGTH = 55

export function AdminContactMessages({
  messages,
  loading,
  exporting,
  onExport,
}: {
  messages: ContactMessage[]
  loading: boolean
  exporting: boolean
  onExport: (format: ExportFormat) => void
}) {
  const [selectedMessage, setSelectedMessage] = useState<ContactMessage | null>(null)

  return (
    <section className="panel span-2">
      <div className="admin-section-header">
        <div>
          <h2>Messages contact</h2>
          <p className="muted">Tous les messages envoyes depuis le formulaire de contact.</p>
        </div>
        <div className="admin-export-actions">
          <button className="button ghost" disabled={exporting || messages.length === 0} type="button" onClick={() => onExport('csv')}>
            Export CSV
          </button>
          <button className="button primary" disabled={exporting || messages.length === 0} type="button" onClick={() => onExport('excel')}>
            Export Excel
          </button>
        </div>
      </div>

      {loading ? (
        <p className="empty-state">Chargement des messages...</p>
      ) : messages.length === 0 ? (
        <p className="empty-state">Aucun message recu pour le moment.</p>
      ) : (
        <div className="admin-table-wrapper">
          <table className="admin-table contact-messages-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Contact</th>
                <th>Telephone</th>
                <th>Entreprise</th>
                <th>Message</th>
              </tr>
            </thead>
            <tbody>
              {messages.map((message) => (
                <tr key={message.id}>
                  <td>{formatDate(message.createdAt)}</td>
                  <td>
                    <div className="admin-user-cell">
                      <span className="admin-user-avatar">{initials(message.name)}</span>
                      <div>
                        <strong>{message.name}</strong>
                        <span>{message.email}</span>
                      </div>
                    </div>
                  </td>
                  <td>{message.phone || 'Non renseigne'}</td>
                  <td>{message.company || 'Non renseignee'}</td>
                  <td>
                    <div className="contact-message-preview-cell">
                      <span className="contact-message-preview">{truncate(message.message)}</span>
                      <button
                        aria-label={`Voir le message de ${message.name}`}
                        className="icon-button view"
                        title="Voir le message"
                        type="button"
                        onClick={() => setSelectedMessage(message)}
                      >
                        <EyeIcon />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {selectedMessage && <ContactMessageModal message={selectedMessage} onClose={() => setSelectedMessage(null)} />}
    </section>
  )
}

function ContactMessageModal({
  message,
  onClose,
}: {
  message: ContactMessage
  onClose: () => void
}) {
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [onClose])

  return (
    <div className="user-detail-modal-backdrop" onMouseDown={onClose}>
      <section
        aria-labelledby="contact-message-title"
        aria-modal="true"
        className="user-detail-panel user-detail-modal contact-message-modal"
        role="dialog"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="user-detail-header">
          <div>
            <p className="eyebrow">Message contact</p>
            <h3 id="contact-message-title">{message.name}</h3>
            <span>{message.email} - {formatDate(message.createdAt)}</span>
          </div>
          <button className="icon-button" aria-label="Fermer le message" title="Fermer" type="button" onClick={onClose}>
            <XIcon />
          </button>
        </div>
        <dl className="user-detail-grid">
          <div><dt>Telephone</dt><dd>{message.phone || 'Non renseigne'}</dd></div>
          <div><dt>Entreprise</dt><dd>{message.company || 'Non renseignee'}</dd></div>
        </dl>
        <div className="contact-message-full">
          <span>Message</span>
          <p>{message.message}</p>
        </div>
      </section>
    </div>
  )
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function initials(value: string) {
  return value
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}

function truncate(value: string) {
  if (value.length <= MESSAGE_PREVIEW_LENGTH) {
    return value
  }
  return `${value.slice(0, MESSAGE_PREVIEW_LENGTH).trim()}...`
}
