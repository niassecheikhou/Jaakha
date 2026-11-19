import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { FormError } from '../../components/FormError'
import { IdentityDocumentUploader } from '../../components/IdentityDocumentUploader'
import { ProfilePhotoUploader } from '../../components/ProfilePhotoUploader'
import { useToast } from '../../components/toast-context'
import { accountApi, getApiError } from '../../services/api'
import { useAuth } from '../../hooks/use-auth'
import type { AccountProfile, AccountProfilePayload } from '../../types/api'

type AccountProfileFormProps = {
  title: string
  statusText?: string
  onSaved?: (payload: AccountProfilePayload, profile: AccountProfile) => Promise<void>
}

export function AccountProfileForm({ title, statusText, onSaved }: AccountProfileFormProps) {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const profileQuery = useQuery({
    queryKey: ['account', 'me'],
    queryFn: accountApi.mine,
    retry: false,
  })

  const [photoUrl, setPhotoUrl] = useState<string>()
  const [bio, setBio] = useState<string>()
  const [zone, setZone] = useState<string>()
  const [monthlyRate, setMonthlyRate] = useState<number>()
  const [experienceYears, setExperienceYears] = useState<number>()
  const [skills, setSkills] = useState<string>()
  const [guardianFullName, setGuardianFullName] = useState<string>()
  const [guardianPhone, setGuardianPhone] = useState<string>()
  const [error, setError] = useState({ message: '', fields: {} })

  const profile = profileQuery.data
  const currentPhoto = photoUrl || profile?.photoUrl || ''
  const currentBio = bio ?? profile?.bio ?? ''
  const currentZone = zone ?? profile?.zone ?? ''
  const currentMonthlyRate = monthlyRate ?? Number(profile?.monthlyRate ?? 0)
  const currentExperienceYears = experienceYears ?? profile?.experienceYears ?? 0
  const currentSkills = skills ?? profile?.skills?.join(', ') ?? ''
  const currentGuardianFullName = guardianFullName ?? profile?.guardianFullName ?? ''
  const currentGuardianPhone = guardianPhone ?? profile?.guardianPhone ?? '+22177'
  const isProviderAccount = user?.role === 'PRESTATAIRE'

  const saveMutation = useMutation({
    mutationFn: async (payload: AccountProfilePayload) => {
      const savedProfile = await accountApi.save(payload)
      await onSaved?.(payload, savedProfile)
      return savedProfile
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['account', 'me'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      setError({ message: '', fields: {} })
      toast.success('Dossier enregistre', 'Les informations du profil ont ete mises a jour.')
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Dossier non enregistre', apiError.message)
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError({ message: '', fields: {} })
    saveMutation.mutate({
      bio: isProviderAccount ? currentBio : '',
      zone: currentZone,
      monthlyRate: isProviderAccount ? currentMonthlyRate : null,
      experienceYears: isProviderAccount ? currentExperienceYears : 0,
      skills: isProviderAccount ? currentSkills.split(',').map((item) => item.trim()).filter(Boolean) : [],
      guardianFullName: isProviderAccount ? currentGuardianFullName : '',
      guardianPhone: isProviderAccount ? currentGuardianPhone : '',
    })
  }

  return (
    <section className="panel">
      <h2>{title}</h2>
      {statusText && <p className="muted">{statusText}</p>}
      <form className="form-grid" onSubmit={handleSubmit}>
        <FormError message={error.message} fields={error.fields} />
        <ProfilePhotoUploader currentPhoto={currentPhoto} onUploaded={setPhotoUrl} />
        <IdentityDocumentUploader frontUrl={profile?.identityPhotoUrl} backUrl={profile?.identityPhotoBackUrl} />
        {user?.role === 'EMPLOYEUR' && (
          <div className="account-summary span-2">
            <div><span>Nom complet</span><strong>{user.fullName}</strong></div>
            <div><span>Telephone</span><strong>{user.phone || 'Non renseigne'}</strong></div>
          </div>
        )}
        <label>
          Zone
          <input value={currentZone} onChange={(event) => setZone(event.target.value)} required />
        </label>
        {isProviderAccount && (
          <>
            <label>
              Tarif mensuel
              <input type="number" min="1" value={currentMonthlyRate} onChange={(event) => setMonthlyRate(Number(event.target.value))} required />
            </label>
            <label>
              Experience
              <input type="number" min="0" value={currentExperienceYears} onChange={(event) => setExperienceYears(Number(event.target.value))} />
            </label>
            <label>
              Telephone du tuteur
              <input
                value={currentGuardianPhone}
                onChange={(event) => setGuardianPhone(event.target.value)}
                pattern="^\+221(70|71|76|77|78)[0-9]{7}$"
                required
              />
            </label>
            <label className="span-2">
              Nom complet du tuteur
              <input value={currentGuardianFullName} onChange={(event) => setGuardianFullName(event.target.value)} required />
            </label>
            <label className="span-2">
              Competences separees par virgule
              <input placeholder="menage, cuisine, garde enfant" value={currentSkills} onChange={(event) => setSkills(event.target.value)} required />
            </label>
            <label className="span-2">
              Bio
              <textarea value={currentBio} onChange={(event) => setBio(event.target.value)} rows={4} required />
            </label>
          </>
        )}
        <button className="button primary" type="submit" disabled={saveMutation.isPending}>
          Enregistrer le dossier
        </button>
      </form>
    </section>
  )
}
