export type AccountProfilePayload = {
  bio: string
  zone: string
  monthlyRate: number | null
  experienceYears: number
  skills: string[]
  guardianFullName: string
  guardianPhone: string
  // Optionnelle : absente, l'adresse deja enregistree est conservee.
  address?: string
}
