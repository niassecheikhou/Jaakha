import type { Booking } from '../../types/api'
import { EmployerBookingsTable } from './EmployerBookingsTable'

export function EmployerBookingHistory({
  bookings,
  loading,
  statusLoading,
  onCancel,
  onSelect,
}: {
  bookings: Booking[]
  loading: boolean
  statusLoading: boolean
  onCancel: (booking: Booking) => void
  onSelect: (booking: Booking) => void
}) {
  return (
    <section className="employer-bookings-panel">
      {loading ? (
        <p className="empty-state">Chargement des reservations...</p>
      ) : (
        <EmployerBookingsTable
          bookings={bookings}
          loading={statusLoading}
          onCancel={onCancel}
          onSelect={onSelect}
        />
      )}
    </section>
  )
}
