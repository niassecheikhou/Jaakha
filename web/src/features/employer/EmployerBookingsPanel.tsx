import { useMemo, useState } from 'react'
import type { Booking } from '../../types/api'
import { formatStatus } from '../../utils/format'
import { EmployerBookingsTable } from './EmployerBookingsTable'
import { activeBookingStatusFilters, bookingReference } from './employer-booking-utils'
import type { BookingStatusFilter } from './employer-types'

export function EmployerBookingsPanel({
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
  const [searchDraft, setSearchDraft] = useState('')
  const [statusDraft, setStatusDraft] = useState<BookingStatusFilter>('ALL')
  const [dateDraft, setDateDraft] = useState('')
  const [filters, setFilters] = useState({
    query: '',
    status: 'ALL' as BookingStatusFilter,
    date: '',
  })
  const filteredBookings = useMemo(() => {
    const normalizedQuery = filters.query.trim().toLowerCase()

    return bookings.filter((booking) => {
      const reference = bookingReference(booking.id).toLowerCase()
      const matchesQuery = !normalizedQuery
        || reference.includes(normalizedQuery)
        || booking.providerName.toLowerCase().includes(normalizedQuery)
        || booking.address.toLowerCase().includes(normalizedQuery)
      const matchesStatus = filters.status === 'ALL' || booking.status === filters.status
      const matchesDate = !filters.date || booking.serviceDate === filters.date

      return matchesQuery && matchesStatus && matchesDate
    })
  }, [bookings, filters])

  function applyFilters() {
    setFilters({
      query: searchDraft,
      status: statusDraft,
      date: dateDraft,
    })
  }

  return (
    <section className="employer-bookings-panel">
      <div className="employer-bookings-filters">
        <input
          aria-label="Rechercher une reservation"
          placeholder="Rechercher l'identifiant"
          value={searchDraft}
          onChange={(event) => setSearchDraft(event.target.value)}
        />
        <select
          aria-label="Filtrer par statut"
          value={statusDraft}
          onChange={(event) => setStatusDraft(event.target.value as BookingStatusFilter)}
        >
          <option value="ALL">Statut Tous</option>
          {activeBookingStatusFilters.map((status) => (
            <option key={status} value={status}>
              {formatStatus(status)}
            </option>
          ))}
        </select>
        <input
          aria-label="Filtrer par date"
          type="date"
          value={dateDraft}
          onChange={(event) => setDateDraft(event.target.value)}
        />
        <button className="button primary" type="button" onClick={applyFilters}>
          Filtre
        </button>
      </div>
      {loading ? (
        <p className="empty-state">Chargement des reservations...</p>
      ) : (
        <EmployerBookingsTable
          bookings={filteredBookings}
          emptyMessage="Il y'a pas encore de demande"
          loading={statusLoading}
          onCancel={onCancel}
          onSelect={onSelect}
        />
      )}
    </section>
  )
}
