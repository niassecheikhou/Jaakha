import type { BookingStatus, DayOfWeek, ProviderStatus } from '../types/api'

const statusLabels: Record<BookingStatus | ProviderStatus, string> = {
  REQUESTED: 'Demandée',
  ACCEPTED: 'Acceptée',
  IN_PROGRESS: 'En cours',
  DONE: 'Terminée',
  CANCELLED: 'Annulée',
  PENDING: 'En attente',
  APPROVED: 'Validé',
  REJECTED: 'Rejeté',
}

const dayLabels: Record<DayOfWeek, string> = {
  MONDAY: 'Lundi',
  TUESDAY: 'Mardi',
  WEDNESDAY: 'Mercredi',
  THURSDAY: 'Jeudi',
  FRIDAY: 'Vendredi',
  SATURDAY: 'Samedi',
  SUNDAY: 'Dimanche',
}

export function formatPrice(value: number) {
  return new Intl.NumberFormat('fr-SN', {
    style: 'currency',
    currency: 'XOF',
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatStatus(status: BookingStatus | ProviderStatus) {
  return statusLabels[status] ?? status
}

export function formatDay(day: DayOfWeek) {
  return dayLabels[day] ?? day
}
