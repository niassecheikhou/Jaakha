import { useQuery } from '@tanstack/react-query'
import { AccountProfileForm } from '../account/AccountProfileForm'
import { providersApi } from '../../services/api'
import type { AccountProfilePayload, Availability, DayOfWeek } from '../../types/api'

const weekDays: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

export function ProviderProfileForm() {
  const profileQuery = useQuery({
    queryKey: ['providers', 'me'],
    queryFn: providersApi.mine,
    retry: false,
  })

  async function syncProviderProfile(payload: AccountProfilePayload) {
    await providersApi.saveMine({
      photoUrl: payload.photoUrl,
      bio: payload.bio,
      zone: payload.zone,
      monthlyRate: payload.monthlyRate ?? 0,
      experienceYears: payload.experienceYears,
      skills: payload.skills,
      availability: defaultAvailability(),
    })
  }

  return (
    <AccountProfileForm
      title="Mon dossier prestataire"
      statusText={profileQuery.data ? `Statut du profil public: ${profileQuery.data.status}` : undefined}
      onSaved={syncProviderProfile}
    />
  )
}

function defaultAvailability(): Availability[] {
  return weekDays.map((dayOfWeek) => ({
    dayOfWeek,
    startTime: '08:00',
    endTime: '18:00',
  }))
}
