export type AccountProfile = {
  id: string
  userId: string
  photoUrl: string | null
  // Recto et verso de la piece d'identite (routes protegees, null si non deposes).
  identityPhotoUrl: string | null
  identityPhotoBackUrl: string | null
  bio: string | null
  zone: string | null
  monthlyRate: number | null
  experienceYears: number
  skills: string[]
  guardianFullName: string | null
  guardianPhone: string | null
}
