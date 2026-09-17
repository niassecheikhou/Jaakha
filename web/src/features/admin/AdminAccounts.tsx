import { useEffect } from 'react'
import type { User } from '../../types/api'
import { formatPrice } from '../../utils/format'
import { ArchiveIcon, CheckIcon, EyeIcon, RestoreIcon, XIcon } from './AdminIcons'
import { displayRole, initials, isProfileComplete } from './admin-utils'

function AccountStatusBadge({ active }: { active: boolean }) {
  return <span className={`status ${active ? 'status-approved' : 'status-rejected'}`}>{active ? 'Valide' : 'Invalide'}</span>
}

function AccountLifecycleBadge({ user }: { user: User }) {
  if (user.archived) {
    return <span className="status status-archived">Archive</span>
  }
  return <AccountStatusBadge active={user.active} />
}

function ProfileCompletionBadge({ user }: { user: User }) {
  const complete = isProfileComplete(user)
  return <span className={`status ${complete ? 'status-approved' : 'status-rejected'}`}>{complete ? 'Dossier complet' : 'Dossier incomplet'}</span>
}

export function UserDetailPanel({
  user,
  onClose,
}: {
  user: User
  onClose: () => void
}) {
  const isProvider = user.role === 'PRESTATAIRE'

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
        aria-labelledby="user-detail-title"
        aria-modal="true"
        className="user-detail-panel user-detail-modal"
        role="dialog"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="user-detail-header">
          <div>
            <p className="eyebrow">Verification</p>
            <h3 id="user-detail-title">{user.fullName}</h3>
            <span>{displayRole(user.role)} - {user.email || user.phone}</span>
          </div>
          <button className="icon-button" aria-label="Fermer le detail utilisateur" title="Fermer" type="button" onClick={onClose}>
            <XIcon />
          </button>
        </div>
        <div className="user-detail-media">
          <div>
            <span>Photo du profil</span>
            {user.photoUrl ? <img src={user.photoUrl} alt={`Photo de ${user.fullName}`} /> : <strong>Non renseignee</strong>}
          </div>
          <div>
            <span>Piece d'identite</span>
            {user.identityPhotoUrl ? <img src={user.identityPhotoUrl} alt={`Piece d'identite de ${user.fullName}`} /> : <strong>Non renseignee</strong>}
          </div>
        </div>
        <dl className="user-detail-grid">
          <div><dt>Nom complet</dt><dd>{user.fullName || 'Non renseigne'}</dd></div>
          <div><dt>Telephone</dt><dd>{user.phone || 'Non renseigne'}</dd></div>
          <div><dt>Zone</dt><dd>{user.zone || 'Non renseignee'}</dd></div>
          {isProvider && (
            <>
              <div><dt>Tarif mensuel</dt><dd>{user.monthlyRate ? `${formatPrice(user.monthlyRate)}/mois` : 'Non renseigne'}</dd></div>
              <div><dt>Experience</dt><dd>{user.experienceYears || 0} an(s)</dd></div>
              <div><dt>Competences</dt><dd>{user.skills.length > 0 ? user.skills.join(', ') : 'Non renseignees'}</dd></div>
              <div><dt>Tuteur</dt><dd>{user.guardianFullName || 'Non renseigne'}</dd></div>
              <div><dt>Telephone du tuteur</dt><dd>{user.guardianPhone || 'Non renseigne'}</dd></div>
              <div className="span-2"><dt>Bio</dt><dd>{user.bio || 'Non renseignee'}</dd></div>
            </>
          )}
        </dl>
        <ProfileCompletionBadge user={user} />
      </section>
    </div>
  )
}

export function AccountsTable({
  users,
  loading,
  onToggle,
  onArchive,
  onInspect,
}: {
  users: User[]
  loading: boolean
  onToggle: (user: User, active: boolean) => void
  onArchive: (user: User, archived: boolean) => void
  onInspect: (user: User) => void
}) {
  return (
    <div className="admin-table-wrapper">
      <table className="admin-table">
        <thead>
          <tr>
            <th>Utilisateur</th>
            <th>Role</th>
            <th>Email</th>
            <th>Telephone</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td>
                <div className="admin-user-cell">
                  <span className="admin-user-avatar">{initials(user.fullName)}</span>
                  <div>
                    <strong>{user.fullName}</strong>
                    <span>{user.id.slice(0, 8)}</span>
                  </div>
                </div>
              </td>
              <td><span className={`role-pill role-${user.role.toLowerCase()}`}>{displayRole(user.role)}</span></td>
              <td>{user.email || 'Non renseigne'}</td>
              <td>{user.phone || 'Non renseigne'}</td>
              <td><AccountLifecycleBadge user={user} /></td>
              <td>
                {user.role === 'ADMIN' ? (
                  <span className="muted">Protege</span>
                ) : (
                  <div className="table-actions">
                    <button
                      aria-label={`Voir le detail de ${user.fullName}`}
                      className="icon-button view"
                      title="Voir le detail"
                      type="button"
                      onClick={() => onInspect(user)}
                    >
                      <EyeIcon />
                    </button>
                    <button
                      aria-label={`Valider le compte de ${user.fullName}`}
                      className="icon-button approve"
                      disabled={loading || user.active || user.archived}
                      title="Valider"
                      type="button"
                      onClick={() => onToggle(user, true)}
                    >
                      <CheckIcon />
                    </button>
                    <button
                      aria-label={`Invalider le compte de ${user.fullName}`}
                      className="icon-button reject"
                      disabled={loading || !user.active || user.archived}
                      title="Invalider"
                      type="button"
                      onClick={() => onToggle(user, false)}
                    >
                      <XIcon />
                    </button>
                    <button
                      aria-label={`${user.archived ? 'Restaurer' : 'Archiver'} le compte de ${user.fullName}`}
                      className="icon-button archive"
                      disabled={loading}
                      title={user.archived ? 'Restaurer' : 'Archiver'}
                      type="button"
                      onClick={() => onArchive(user, !user.archived)}
                    >
                      {user.archived ? <RestoreIcon /> : <ArchiveIcon />}
                    </button>
                  </div>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
