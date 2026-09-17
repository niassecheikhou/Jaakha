import type { BookingStatus } from '../../types/api'

export const activeBookingStatusFilters: BookingStatus[] = ['REQUESTED', 'IN_PROGRESS']

export function bookingReference(id: string) {
  return `#BK-${id.replace(/-/g, '').slice(0, 5).toUpperCase()}`
}

export function formatBookingDate(value: string) {
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(new Date(value))
}

export function formatBookingTime(value: string) {
  return value.slice(0, 5).replace(':', 'h')
}

export function canCancelBooking(status: BookingStatus) {
  return status === 'REQUESTED' || status === 'ACCEPTED'
}
