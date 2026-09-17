import type { BookingStatus } from './booking-status'

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
