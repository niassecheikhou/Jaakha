import { StatusBadge } from '../../components/StatusBadge'
import type { Booking } from '../../types/api'
import { bookingReference, canCancelBooking, formatBookingDate, formatBookingTime } from './employer-booking-utils'

export function EmployerBookingsTable({
  bookings,
  emptyMessage = 'Aucune reservation ne correspond aux filtres.',
  loading,
  onCancel,
  onSelect,
}: {
  bookings: Booking[]
  emptyMessage?: string
  loading: boolean
  onCancel: (booking: Booking) => void
  onSelect: (booking: Booking) => void
}) {
  return (
    <>
      {bookings.length === 0 ? (
        <p className="empty-state">{emptyMessage}</p>
      ) : (
        <div className="employer-bookings-table-wrapper">
          <table className="employer-bookings-table">
            <thead>
              <tr>
                <th>Numero de reservation</th>
                <th>Date</th>
                <th>Temps</th>
                <th>Prestataire</th>
                <th>Statut</th>
                <th>Actes</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((booking) => (
                <tr key={booking.id}>
                  <td>{bookingReference(booking.id)}</td>
                  <td>{formatBookingDate(booking.serviceDate)}</td>
                  <td>{formatBookingTime(booking.startTime)}</td>
                  <td>{booking.providerName}</td>
                  <td><StatusBadge status={booking.status} /></td>
                  <td>
                    <div className="employer-bookings-actions">
                      <button className="button secondary" type="button" onClick={() => onSelect(booking)}>
                        Voir
                      </button>
                      {canCancelBooking(booking.status) && (
                        <button className="button ghost" disabled={loading} type="button" onClick={() => onCancel(booking)}>
                          Annuler
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
