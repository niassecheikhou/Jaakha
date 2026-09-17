import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { StatusBadge } from '../../../components/StatusBadge'
import { WorkspaceShell, type WorkspaceNavItem } from '../../../components/WorkspaceShell'
import { useAuth } from '../../../hooks/use-auth'
import { bookingsApi } from '../../../services/api'
import { ProviderProfileForm } from './ProviderProfileForm'

type ProviderMenu = 'requests' | 'profile'

function RequestsIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M6 8h12l-1 12H7L6 8Z" />
      <path d="M9 8a3 3 0 0 1 6 0" />
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

export function ProviderDashboard() {
  const { user } = useAuth()
  const [activeMenu, setActiveMenu] = useState<ProviderMenu>('requests')
  const bookingsQuery = useQuery({
    queryKey: ['bookings', 'me'],
    queryFn: bookingsApi.mine,
  })
  const bookings = bookingsQuery.data ?? []
  const pendingBookings = bookings.filter((booking) => booking.status === 'REQUESTED')
  const providerItems: WorkspaceNavItem<ProviderMenu>[] = [
    {
      id: 'requests',
      label: 'Demandes recues',
      icon: <RequestsIcon />,
      badge: pendingBookings.length.toLocaleString('fr-FR'),
    },
    {
      id: 'profile',
      label: 'Mon profil',
      icon: <ProfileIcon />,
    },
  ]

  return (
    <WorkspaceShell
      activeItem={activeMenu}
      eyebrow="Prestataire"
      items={providerItems}
      title={activeMenu === 'requests' ? 'Demandes recues' : 'Mon profil'}
      userContact={user?.email || user?.phone || undefined}
      userName={user?.fullName || 'Prestataire'}
      onSelect={setActiveMenu}
    >
      {user && !user.active && (
        <div className="notice">
          <strong>Validation en attente</strong>
          <span>Complete ton dossier. L'administration pourra ensuite valider ton compte.</span>
        </div>
      )}
      {activeMenu === 'requests' && (
        <section className="panel">
          <div className="stack">
            {bookings.length === 0 && <p className="empty-state">Aucune demande recue.</p>}
            {bookings.map((booking) => (
              <article className="row-card" key={booking.id}>
                <div>
                  <h2>{booking.employerName}</h2>
                  <p>
                    {booking.serviceDate} a {booking.startTime} - {booking.durationHours}h
                  </p>
                  <p>{booking.address}</p>
                </div>
                <StatusBadge status={booking.status} />
              </article>
            ))}
          </div>
        </section>
      )}
      {activeMenu === 'profile' && <ProviderProfileForm />}
    </WorkspaceShell>
  )
}
