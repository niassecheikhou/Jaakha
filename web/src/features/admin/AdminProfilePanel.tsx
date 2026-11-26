import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { FormError } from '../../components/FormError'
import { IdentityDocumentUploader } from '../../components/IdentityDocumentUploader'
import { ProfilePhotoUploader } from '../../components/ProfilePhotoUploader'
import { useToast } from '../../components/toast-context'
import { useAuth } from '../../hooks/use-auth'
import { accountApi, getApiError } from '../../services/api'

// Profil d'un compte d'administration.
// - Super administrateur : photo et rappel des informations du compte.
// - Administrateur : dossier a completer (piece d'identite recto/verso, adresse, localisation)
//   que le super administrateur verifie avant de valider le compte.
export function AdminProfilePanel() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const isAdmin = user?.role === 'ADMIN'
  const profileQuery = useQuery({
    queryKey: ['account', 'me'],
    queryFn: accountApi.mine,
    retry: false,
  })
  const profile = profileQuery.data
  const [address, setAddress] = useState<string>()
  const [zone, setZone] = useState<string>()
  const [error, setError] = useState({ message: '', fields: {} })
  const currentAddress = address ?? profile?.address ?? ''
  const currentZone = zone ?? profile?.zone ?? ''

  const saveMutation = useMutation({
    mutationFn: () => accountApi.save({
      bio: profile?.bio ?? '',
      zone: currentZone,
      monthlyRate: profile?.monthlyRate ?? null,
      experienceYears: profile?.experienceYears ?? 0,
      skills: profile?.skills ?? [],
      guardianFullName: profile?.guardianFullName ?? '',
      guardianPhone: profile?.guardianPhone ?? '',
      address: currentAddress,
    }),
    onSuccess: () => {
      setError({ message: '', fields: {} })
      void queryClient.invalidateQueries({ queryKey: ['account', 'me'] })
      toast.success('Dossier enregistre', 'Ton adresse et ta localisation ont ete mises a jour.')
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Dossier non enregistre', apiError.message)
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    saveMutation.mutate()
  }

  return (
    <section className="panel">
      <h2>{user?.role === 'SUPER_ADMIN' ? 'Mon profil super administrateur' : 'Mon profil administrateur'}</h2>
      <form className="form-grid" onSubmit={handleSubmit}>
        <FormError message={error.message} fields={error.fields} />
        <ProfilePhotoUploader currentPhoto={profile?.photoUrl} />
        {isAdmin && <IdentityDocumentUploader frontUrl={profile?.identityPhotoUrl} backUrl={profile?.identityPhotoBackUrl} />}
        <div className="account-summary span-2">
          <div><span>Nom complet</span><strong>{user?.fullName || 'Administrateur'}</strong></div>
          <div><span>Email</span><strong>{user?.email || 'Non renseigne'}</strong></div>
          <div><span>Telephone</span><strong>{user?.phone || 'Non renseigne'}</strong></div>
        </div>
        {isAdmin && (
          <>
            <label>
              Adresse
              <input
                value={currentAddress}
                onChange={(event) => setAddress(event.target.value)}
                maxLength={255}
                placeholder="Ex. Rue 10, Medina"
                required
              />
            </label>
            <label>
              Localisation (ville ou quartier)
              <input
                value={currentZone}
                onChange={(event) => setZone(event.target.value)}
                maxLength={160}
                placeholder="Ex. Dakar Plateau"
                required
              />
            </label>
            <button className="button primary" disabled={saveMutation.isPending} type="submit">
              Enregistrer mon dossier
            </button>
          </>
        )}
      </form>
    </section>
  )
}
