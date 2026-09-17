export type AccountProfile = {
  id: string
  userId: string
  photoUrl: string | null
  identityPhotoUrl: string | null
  bio: string | null
  zone: string | null
  monthlyRate: number | null
  experienceYears: number
  skills: string[]
  guardianFullName: string | null
  guardianPhone: string | null
}
