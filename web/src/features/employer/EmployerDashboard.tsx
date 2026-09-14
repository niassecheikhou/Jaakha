import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { StatusBadge } from '../../components/StatusBadge'
import { useToast } from '../../components/toast-context'
import { WorkspaceShell, type WorkspaceNavItem } from '../../components/WorkspaceShell'
import { bookingsApi, getApiError } from '../../services/api'
import { useAuth } from '../../hooks/use-auth'
import { AccountProfileForm } from '../account/AccountProfileForm'
import { ReviewForm } from '../reviews/ReviewForm'
import { formatStatus } from '../../utils/format'
import type { Booking, BookingStatus } from '../../types/api'

type BookingStatusFilter = 'ALL' | BookingStatus
type EmployerMenu = 'bookings' | 'history' | 'profile'

const activeBookingStatusFilters: BookingStatus[] = ['REQUESTED', 'IN_PROGRESS']

function bookingReference(id: string) {
  return `#BK-${id.replace(/-/g, '').slice(0, 5).toUpperCase()}`
}

function formatBookingDate(value: string) {
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(new Date(value))
}

function formatBookingTime(value: string) {
  return value.slice(0, 5).replace(':', 'h')
}

function canCancelBooking(status: BookingStatus) {
  return status === 'REQUESTED' || status === 'ACCEPTED'
}

function EmployerBookingsTable({
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
                <th>Numéro de réservation</th>
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

function BookingDetailModal({
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
            <p className="eyebrow">Détail réservation</p>
            <h3 id="employer-booking-detail-title">{bookingReference(booking.id)}</h3>
            <span>{booking.providerName} - {formatBookingDate(booking.serviceDate)} à {formatBookingTime(booking.startTime)}</span>
          </div>
          <button className="icon-button" aria-label="Fermer le détail réservation" title="Fermer" type="button" onClick={onClose}>
            <XIcon />
          </button>
        </div>
        <div className="employer-booking-detail">
          <div>
            <span>Réservation</span>
            <strong>{bookingReference(booking.id)}</strong>
          </div>
          <div>
            <span>Adresse</span>
            <strong>{booking.address}</strong>
          </div>
          <div>
            <span>Durée</span>
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

function XIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  )
}

function ProfileIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </svg>
  )
}

function BookingIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M6 8h12l-1 12H7L6 8Z" />
      <path d="M9 8a3 3 0 0 1 6 0" />
    </svg>
  )
}

function HistoryIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M8 3h8l2 3v15H6V6l2-3Z" />
      <path d="M9 11h6M9 15h6" />
    </svg>
  )
}

export function EmployerDashboard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [activeMenu, setActiveMenu] = useState<EmployerMenu>('bookings')
  const [searchDraft, setSearchDraft] = useState('')
  const [statusDraft, setStatusDraft] = useState<BookingStatusFilter>('ALL')
  const [dateDraft, setDateDraft] = useState('')
  const [filters, setFilters] = useState({
    query: '',
    status: 'ALL' as BookingStatusFilter,
    date: '',
  })
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null)
  const bookingsQuery = useQuery({
    queryKey: ['bookings', 'me'],
    queryFn: bookingsApi.mine,
  })
  const statusMutation = useMutation({
    mutationFn: ({ id }: { bookingReference: string; id: string }) => bookingsApi.updateStatus(id, 'CANCELLED'),
    onSuccess: (_, variables) => {
      toast.success('Reservation annulee', `${variables.bookingReference} a ete annulee avec succes.`)
      void queryClient.invalidateQueries({ queryKey: ['bookings', 'me'] })
    },
    onError: (caughtError) => toast.error('Annulation impossible', getApiError(caughtError).message),
  })
  const sortedBookings = useMemo(() => {
    return [...(bookingsQuery.data ?? [])].sort((first, second) => (
      new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime()
    ))
  }, [bookingsQuery.data])
  const activeBookings = useMemo(() => (
    sortedBookings.filter((booking) => (
      booking.status === 'REQUESTED'
      || booking.status === 'IN_PROGRESS'
    ))
  ), [sortedBookings])
  const filteredBookings = useMemo(() => {
    const normalizedQuery = filters.query.trim().toLowerCase()

    return activeBookings.filter((booking) => {
      const reference = bookingReference(booking.id).toLowerCase()
      const matchesQuery = !normalizedQuery
        || reference.includes(normalizedQuery)
        || booking.providerName.toLowerCase().includes(normalizedQuery)
        || booking.address.toLowerCase().includes(normalizedQuery)
      const matchesStatus = filters.status === 'ALL' || booking.status === filters.status
      const matchesDate = !filters.date || booking.serviceDate === filters.date

      return matchesQuery && matchesStatus && matchesDate
    })
  }, [activeBookings, filters])
  const historyBookings = sortedBookings
  const activeTitle = activeMenu === 'profile'
    ? 'Mon profil'
    : activeMenu === 'history'
      ? 'Historique des reservations'
      : 'Mes reservations'
  const employerItems: WorkspaceNavItem<EmployerMenu>[] = [
    {
      id: 'bookings',
      label: 'Ma reservation',
      icon: <BookingIcon />,
      badge: activeBookings.length.toLocaleString('fr-FR'),
    },
    {
      id: 'history',
      label: 'Historique',
      icon: <HistoryIcon />,
      badge: sortedBookings.length.toLocaleString('fr-FR'),
    },
    {
      id: 'profile',
      label: 'Mon profil',
      icon: <ProfileIcon />,
    },
  ]

  function applyFilters() {
    setFilters({
      query: searchDraft,
      status: statusDraft,
      date: dateDraft,
    })
  }

  return (
    <WorkspaceShell
      activeItem={activeMenu}
      eyebrow="Employeur"
      items={employerItems}
      title={activeTitle}
      userContact={user?.email || user?.phone || undefined}
      userName={user?.fullName || 'Employeur'}
      onSelect={setActiveMenu}
    >
      {user && !user.active && (
        <div className="notice">
          <strong>Validation en attente</strong>
          <span>Complete ton dossier. L'administration pourra ensuite valider ton compte.</span>
        </div>
      )}
      {activeMenu === 'profile' && <AccountProfileForm title="Mon dossier employeur" />}
      {activeMenu === 'bookings' && (
      <section className="employer-bookings-panel">
        <div className="employer-bookings-filters">
          <input
            aria-label="Rechercher une réservation"
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
        {bookingsQuery.isLoading ? (
          <p className="empty-state">Chargement des réservations...</p>
        ) : (
          <EmployerBookingsTable
            bookings={filteredBookings}
            emptyMessage="Il y'a pas encore de demande"
            loading={statusMutation.isPending}
            onCancel={(booking) => statusMutation.mutate({ id: booking.id, bookingReference: bookingReference(booking.id) })}
            onSelect={setSelectedBooking}
          />
        )}
      </section>
      )}
      {activeMenu === 'history' && (
        <section className="employer-bookings-panel">
          {bookingsQuery.isLoading ? (
            <p className="empty-state">Chargement des reservations...</p>
          ) : (
            <EmployerBookingsTable
              bookings={historyBookings}
              loading={statusMutation.isPending}
              onCancel={(booking) => statusMutation.mutate({ id: booking.id, bookingReference: bookingReference(booking.id) })}
              onSelect={setSelectedBooking}
            />
          )}
        </section>
      )}
      {selectedBooking && (
        <BookingDetailModal booking={selectedBooking} onClose={() => setSelectedBooking(null)} />
      )}
    </WorkspaceShell>
  )
}
