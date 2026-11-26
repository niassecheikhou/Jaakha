import { useQuery } from '@tanstack/react-query'
import { ProfilePhotoUploader } from '../../components/ProfilePhotoUploader'
import { useAuth } from '../../hooks/use-auth'
import { accountApi } from '../../services/api'

// Profil de l'administrateur : photo de profil et rappel des informations du compte.
// Pas de piece d'identite ni de validation de dossier pour un compte ADMIN.
export function AdminProfilePanel() {
  const { user } = useAuth()
  const profileQuery = useQuery({
    queryKey: ['account', 'me'],
    queryFn: accountApi.mine,
    retry: false,
  })

  return (
    <section className="panel">
      <h2>{user?.role === 'SUPER_ADMIN' ? 'Mon profil super administrateur' : 'Mon profil administrateur'}</h2>
      <div className="form-grid">
        <ProfilePhotoUploader currentPhoto={profileQuery.data?.photoUrl} />
        <div className="account-summary span-2">
          <div><span>Nom complet</span><strong>{user?.fullName || 'Administrateur'}</strong></div>
          <div><span>Email</span><strong>{user?.email || 'Non renseigne'}</strong></div>
          <div><span>Telephone</span><strong>{user?.phone || 'Non renseigne'}</strong></div>
        </div>
      </div>
    </section>
  )
}
