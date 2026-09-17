import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useToast } from '../../components/toast-context'
import { WorkspaceShell, type WorkspaceNavItem } from '../../components/WorkspaceShell'
import { useAuth } from '../../hooks/use-auth'
import { bookingsApi, getApiError } from '../../services/api'
import type { Booking } from '../../types/api'
import { AccountProfileForm } from '../account/AccountProfileForm'
import { EmployerBookingDetailModal } from './EmployerBookingDetailModal'
import { EmployerBookingHistory } from './EmployerBookingHistory'
import { EmployerBookingsPanel } from './EmployerBookingsPanel'
import { BookingIcon, HistoryIcon, ProfileIcon } from './EmployerIcons'
import { bookingReference } from './employer-booking-utils'
import type { EmployerMenu } from './employer-types'

export function EmployerDashboard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [activeMenu, setActiveMenu] = useState<EmployerMenu>('bookings')
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

  function cancelBooking(booking: Booking) {
    statusMutation.mutate({ id: booking.id, bookingReference: bookingReference(booking.id) })
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
        <EmployerBookingsPanel
          bookings={activeBookings}
          loading={bookingsQuery.isLoading}
          statusLoading={statusMutation.isPending}
          onCancel={cancelBooking}
          onSelect={setSelectedBooking}
        />
      )}
      {activeMenu === 'history' && (
        <EmployerBookingHistory
          bookings={sortedBookings}
          loading={bookingsQuery.isLoading}
          statusLoading={statusMutation.isPending}
          onCancel={cancelBooking}
          onSelect={setSelectedBooking}
        />
      )}
      {selectedBooking && (
        <EmployerBookingDetailModal booking={selectedBooking} onClose={() => setSelectedBooking(null)} />
      )}
    </WorkspaceShell>
  )
}
