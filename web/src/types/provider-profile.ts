import type { Availability } from './availability'
import type { ProviderStatus } from './provider-status'

export type ProviderProfile = {
  id: string
  userId: string
  fullName: string
  photoUrl: string | null
  bio: string | null
  zone: string
  monthlyRate: number
  experienceYears: number
  skills: string[]
  status: ProviderStatus
  averageRating: number
  reviewCount: number
  availability: Availability[]
}
