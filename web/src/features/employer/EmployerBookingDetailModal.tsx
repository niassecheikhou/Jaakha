import { useEffect } from 'react'
import type { Booking } from '../../types/api'
import { ReviewForm } from '../reviews/ReviewForm'
import { XIcon } from './EmployerIcons'
import { bookingReference, formatBookingDate, formatBookingTime } from './employer-booking-utils'

export function EmployerBookingDetailModal({
  booking,
  onClose,
}: {
  booking: Booking
  onClose: () => void
}) {
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow
    }
  }, [onClose])

  return (
    <div className="employer-booking-modal-backdrop" onMouseDown={onClose}>
      <section
        aria-labelledby="employer-booking-detail-title"
        aria-modal="true"
        className="employer-booking-modal"
        role="dialog"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="employer-booking-modal-header">
          <div>
            <p className="eyebrow">Detail reservation</p>
            <h3 id="employer-booking-detail-title">{bookingReference(booking.id)}</h3>
            <span>{booking.providerName} - {formatBookingDate(booking.serviceDate)} a {formatBookingTime(booking.startTime)}</span>
          </div>
          <button className="icon-button" aria-label="Fermer le detail reservation" title="Fermer" type="button" onClick={onClose}>
            <XIcon />
          </button>
        </div>
        <div className="employer-booking-detail">
          <div>
            <span>Reservation</span>
            <strong>{bookingReference(booking.id)}</strong>
          </div>
          <div>
            <span>Adresse</span>
            <strong>{booking.address}</strong>
          </div>
          <div>
            <span>Duree</span>
            <strong>{booking.durationHours}h</strong>
          </div>
          <div>
            <span>Instructions</span>
            <strong>{booking.instructions || 'Aucune instruction.'}</strong>
          </div>
          {booking.status === 'DONE' && (
            <div className="span-2">
              <span>Avis</span>
              <ReviewForm bookingId={booking.id} />
            </div>
          )}
        </div>
      </section>
    </div>
  )
}
