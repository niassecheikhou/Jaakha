export type Role = 'EMPLOYEUR' | 'PRESTATAIRE' | 'ADMIN'

export type BookingStatus =
  | 'REQUESTED'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'DONE'
  | 'CANCELLED'

export type ProviderStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type ReportStatus = 'OPEN' | 'REVIEWING' | 'RESOLVED' | 'DISMISSED'

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

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
  bio: string | null
  zone: string | null
  monthlyRate: number | null
  experienceYears: number
  skills: string[]
  guardianFullName: string | null
  guardianPhone: string | null
}

export type AuthResponse = {
  token: string
  user: User
}

export type RegisterPayload = {
  fullName: string
  email: string
  phone: string
  password: string
  role: Exclude<Role, 'ADMIN'>
}

export type LoginPayload = {
  identifier: string
  password: string
}

export type AdminCreateUserPayload = {
  fullName: string
  email: string
  phone: string
  password: string
  role: Exclude<Role, 'ADMIN'>
}

export type Availability = {
  id?: string
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
}

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

export type ProviderProfilePayload = {
  photoUrl: string
  bio: string
  zone: string
  monthlyRate: number
  experienceYears: number
  skills: string[]
  availability: Availability[]
}

export type PhotoUploadResponse = {
  photoUrl: string
}

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

export type AccountProfilePayload = {
  photoUrl: string
  identityPhotoUrl: string
  bio: string
  zone: string
  monthlyRate: number | null
  experienceYears: number
  skills: string[]
  guardianFullName: string
  guardianPhone: string
}

export type Booking = {
  id: string
  status: BookingStatus
  serviceDate: string
  startTime: string
  durationHours: number
  address: string
  instructions: string | null
  employerId: string
  employerName: string
  providerId: string
  providerName: string
  createdAt: string
}

export type BookingPayload = {
  providerId: string
}

export type Review = {
  id: string
  rating: number
  comment: string | null
  employerId: string
  employerName: string
  createdAt: string
}

export type ReviewPayload = {
  bookingId: string
  rating: number
  comment: string
}

export type Notification = {
  id: string
  type: string
  title: string
  message: string
  bookingId: string | null
  userId: string | null
  read: boolean
  createdAt: string
}

export type ApiError = {
  status: number
  message: string
  fields?: Record<string, string>
}
