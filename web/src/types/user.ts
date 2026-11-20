import type { Role } from './role'

export type User = {
  id: string
  fullName: string
  email: string | null
  phone: string | null
  role: Role
  emailVerified: boolean
  active: boolean
  archived: boolean
  photoUrl: string | null
  identityPhotoUrl: string | null
  identityPhotoBackUrl: string | null
  bio: string | null
  zone: string | null
  monthlyRate: number | null
  experienceYears: number
  skills: string[]
  guardianFullName: string | null
  guardianPhone: string | null
  // Date d'inscription (ISO-8601).
  createdAt: string | null
}
