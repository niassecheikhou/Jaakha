import type { Availability } from './availability'

export type ProviderProfilePayload = {
  bio: string
  zone: string
  monthlyRate: number
  experienceYears: number
  skills: string[]
  availability: Availability[]
}
