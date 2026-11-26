import { useEffect } from 'react'
import { ActionsMenu, type ActionsMenuItem } from '../../components/ActionsMenu'
import { ProtectedImage } from '../../components/ProtectedImage'
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
  // Le super administrateur n'a pas de dossier ; un administrateur en a un (CNI, adresse, localisation).
  const isAdministration = user.role === 'SUPER_ADMIN'
  const isAdmin = user.role === 'ADMIN'

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
          {!isAdministration && <div className="identity-document">
            <span>CNI recto{user.identityPhotoUrl ? ' (cliquer pour agrandir)' : ''}</span>
            {user.identityPhotoUrl ? <ProtectedImage src={user.identityPhotoUrl} alt={`Recto de la piece d'identite de ${user.fullName}`} zoomable /> : <strong>Non renseigne</strong>}
          </div>}
          {!isAdministration && <div className="identity-document">
            <span>CNI verso{user.identityPhotoBackUrl ? ' (cliquer pour agrandir)' : ''}</span>
            {user.identityPhotoBackUrl ? <ProtectedImage src={user.identityPhotoBackUrl} alt={`Verso de la piece d'identite de ${user.fullName}`} zoomable /> : <strong>Non renseigne</strong>}
          </div>}
        </div>
        <dl className="user-detail-grid">
          <div><dt>Nom complet</dt><dd>{user.fullName || 'Non renseigne'}</dd></div>
          <div><dt>Telephone</dt><dd>{user.phone || 'Non renseigne'}</dd></div>
          <div><dt>{isAdmin ? 'Localisation' : 'Zone'}</dt><dd>{user.zone || 'Non renseignee'}</dd></div>
          {isAdmin && <div className="span-2"><dt>Adresse</dt><dd>{user.address || 'Non renseignee'}</dd></div>}
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
        {!isAdministration && <ProfileCompletionBadge user={user} />}
      </section>
    </div>
  )
}

// Actions applicables au compte selon son etat (archive, valide ou en attente).
function accountActions(
  user: User,
  loading: boolean,
  onInspect: (user: User) => void,
  onToggle: (user: User, active: boolean) => void,
  onArchive: (user: User, archived: boolean) => void,
): ActionsMenuItem[] {
  const items: ActionsMenuItem[] = [
    { id: 'view', label: 'Voir le detail', icon: <EyeIcon />, tone: 'view', onSelect: () => onInspect(user) },
  ]
  if (!user.archived) {
    items.push(user.active
      ? { id: 'invalidate', label: 'Invalider le compte', icon: <XIcon />, tone: 'reject', disabled: loading, onSelect: () => onToggle(user, false) }
      : { id: 'validate', label: 'Valider le compte', icon: <CheckIcon />, tone: 'approve', disabled: loading, onSelect: () => onToggle(user, true) })
  }
  items.push(user.archived
    ? { id: 'restore', label: 'Restaurer le compte', icon: <RestoreIcon />, tone: 'archive', disabled: loading, onSelect: () => onArchive(user, false) }
    : { id: 'archive', label: 'Archiver le compte', icon: <ArchiveIcon />, tone: 'archive', disabled: loading, onSelect: () => onArchive(user, true) })
  return items
}

// Un compte super administrateur est toujours protege ; un compte administrateur n'est
// gerable que par le super administrateur (canManageAdmins).
function isProtectedAccount(user: User, canManageAdmins: boolean) {
  return user.role === 'SUPER_ADMIN' || (user.role === 'ADMIN' && !canManageAdmins)
}

export function AccountsTable({
  users,
  canManageAdmins = false,
  loading,
  onToggle,
  onArchive,
  onInspect,
}: {
  users: User[]
  canManageAdmins?: boolean
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
            <th className="actions-column">Actions</th>
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
              <td className="actions-column">
                {isProtectedAccount(user, canManageAdmins) ? (
                  <span className="muted">Protege</span>
                ) : (
                  <ActionsMenu subject={user.fullName} items={accountActions(user, loading, onInspect, onToggle, onArchive)} />
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
