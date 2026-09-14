import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FormError } from '../../components/FormError'
import { PasswordInput } from '../../components/PasswordInput'
import { StatusBadge } from '../../components/StatusBadge'
import { useToast } from '../../components/toast-context'
import { WorkspaceShell, type WorkspaceNavItem } from '../../components/WorkspaceShell'
import { useAuth } from '../../hooks/use-auth'
import { adminApi, getApiError } from '../../services/api'
import { formatPrice, formatStatus } from '../../utils/format'
import type { AdminCreateUserPayload, Booking, BookingStatus, ProviderProfile, ProviderStatus, Role, User } from '../../types/api'

type AdminMenu = 'dashboard' | 'accounts' | 'bookings'

function AccountStatusBadge({ active }: { active: boolean }) {
  return <span className={`status ${active ? 'status-approved' : 'status-rejected'}`}>{active ? 'Valide' : 'Invalide'}</span>
}

function AccountLifecycleBadge({ user }: { user: User }) {
  if (user.archived) {
    return <span className="status status-archived">Archive</span>
  }
  return <AccountStatusBadge active={user.active} />
}

function ProfileCompletionBadge({ user }: { user: User }) {
  const complete = isProfileComplete(user)
  return <span className={`status ${complete ? 'status-approved' : 'status-rejected'}`}>{complete ? 'Dossier complet' : 'Dossier incomplet'}</span>
}

function UserDetailPanel({
  user,
  onClose,
}: {
  user: User
  onClose: () => void
}) {
  const isProvider = user.role === 'PRESTATAIRE'

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
    <div className="user-detail-modal-backdrop" onMouseDown={onClose}>
      <section
        aria-labelledby="user-detail-title"
        aria-modal="true"
        className="user-detail-panel user-detail-modal"
        role="dialog"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="user-detail-header">
          <div>
            <p className="eyebrow">Verification</p>
            <h3 id="user-detail-title">{user.fullName}</h3>
            <span>{displayRole(user.role)} - {user.email || user.phone}</span>
          </div>
          <button className="icon-button" aria-label="Fermer le detail utilisateur" title="Fermer" type="button" onClick={onClose}>
            <XIcon />
          </button>
        </div>
        <div className="user-detail-media">
          <div>
            <span>Photo du profil</span>
            {user.photoUrl ? <img src={user.photoUrl} alt={`Photo de ${user.fullName}`} /> : <strong>Non renseignee</strong>}
          </div>
          <div>
            <span>Piece d'identite</span>
            {user.identityPhotoUrl ? <img src={user.identityPhotoUrl} alt={`Piece d'identite de ${user.fullName}`} /> : <strong>Non renseignee</strong>}
          </div>
        </div>
        <dl className="user-detail-grid">
          <div><dt>Nom complet</dt><dd>{user.fullName || 'Non renseigne'}</dd></div>
          <div><dt>Telephone</dt><dd>{user.phone || 'Non renseigne'}</dd></div>
          <div><dt>Zone</dt><dd>{user.zone || 'Non renseignee'}</dd></div>
          {isProvider && (
            <>
              <div><dt>Tarif mensuel</dt><dd>{user.monthlyRate ? `${formatPrice(user.monthlyRate)}/mois` : 'Non renseigne'}</dd></div>
              <div><dt>Experience</dt><dd>{user.experienceYears || 0} an(s)</dd></div>
              <div><dt>Competences</dt><dd>{user.skills.length > 0 ? user.skills.join(', ') : 'Non renseignees'}</dd></div>
              <div><dt>Tuteur</dt><dd>{user.guardianFullName || 'Non renseigne'}</dd></div>
              <div><dt>Telephone du tuteur</dt><dd>{user.guardianPhone || 'Non renseigne'}</dd></div>
              <div className="span-2"><dt>Bio</dt><dd>{user.bio || 'Non renseignee'}</dd></div>
            </>
          )}
        </dl>
        <ProfileCompletionBadge user={user} />
      </section>
    </div>
  )
}

function isProfileComplete(user: User) {
  const hasCommonProfile = Boolean(
    user.photoUrl
      && user.identityPhotoUrl
      && user.fullName
      && user.phone
      && user.zone
  )

  if (user.role === 'EMPLOYEUR') {
    return hasCommonProfile
  }

  return Boolean(
    hasCommonProfile
      && user.bio
      && user.monthlyRate
      && user.monthlyRate > 0
      && user.skills.length > 0
      && user.guardianFullName
      && user.guardianPhone,
  )
}

function isProviderAccountPending(user: User) {
  return user.role === 'PRESTATAIRE' && !user.active && !user.archived
}

function countPendingProviderApprovals(users: User[], providers: ProviderProfile[]) {
  const pendingProviderUserIds = new Set(users.filter(isProviderAccountPending).map((user) => user.id))
  const pendingProviderProfileCount = providers.filter((provider) => (
    provider.status === 'PENDING' && !pendingProviderUserIds.has(provider.userId)
  )).length

  return pendingProviderUserIds.size + pendingProviderProfileCount
}

function displayRole(role: User['role']) {
  if (role === 'EMPLOYEUR') {
    return 'Employeur'
  }
  if (role === 'PRESTATAIRE') {
    return 'Prestataire'
  }
  return 'Administrateur'
}

function initials(fullName: string) {
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
}

function CheckIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="m5 12 4 4L19 6" />
    </svg>
  )
}

function XIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  )
}

function ArchiveIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M4 7h16M6 7l1 12h10l1-12M9 11h6M8 7l1-3h6l1 3" />
    </svg>
  )
}

function RestoreIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M4 7v5h5M5 12a7 7 0 1 0 2.05-4.95L4 10" />
    </svg>
  )
}

function EyeIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" />
      <path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
    </svg>
  )
}

function BagIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M6 8h12l-1 12H7L6 8Z" />
      <path d="M9 8a3 3 0 0 1 6 0" />
    </svg>
  )
}

function UsersIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <path d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  )
}

function HourglassIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M6 3h12M6 21h12M8 3c0 5 8 5 8 10s-8 5-8 8M16 3c0 5-8 5-8 10s8 5 8 8" />
    </svg>
  )
}

function ProfilePendingIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
      <path d="M4 21a8 8 0 0 1 12.68-6.49" />
      <path d="M19 15v4h3" />
      <path d="M19 22a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z" />
    </svg>
  )
}

function ActivityIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 7h18s-3 0-3-7" />
      <path d="M10 20a2 2 0 0 0 4 0" />
    </svg>
  )
}

function DashboardIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M4 4h6v6H4V4Z" />
      <path d="M14 4h6v6h-6V4Z" />
      <path d="M4 14h6v6H4v-6Z" />
      <path d="M14 14h6v6h-6v-6Z" />
    </svg>
  )
}

function AccountsTable({
  users,
  loading,
  onToggle,
  onArchive,
  onInspect,
}: {
  users: User[]
  loading: boolean
  onToggle: (user: User, active: boolean) => void
  onArchive: (user: User, archived: boolean) => void
  onInspect: (user: User) => void
}) {
  return (
    <div className="admin-table-wrapper">
      <table className="admin-table">
        <thead>
          <tr>
            <th>Utilisateur</th>
            <th>Role</th>
            <th>Email</th>
            <th>Telephone</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td>
                <div className="admin-user-cell">
                  <span className="admin-user-avatar">{initials(user.fullName)}</span>
                  <div>
                    <strong>{user.fullName}</strong>
                    <span>{user.id.slice(0, 8)}</span>
                  </div>
                </div>
              </td>
              <td><span className={`role-pill role-${user.role.toLowerCase()}`}>{displayRole(user.role)}</span></td>
              <td>{user.email || 'Non renseigne'}</td>
              <td>{user.phone || 'Non renseigne'}</td>
              <td><AccountLifecycleBadge user={user} /></td>
              <td>
                {user.role === 'ADMIN' ? (
                  <span className="muted">Protege</span>
                ) : (
                  <div className="table-actions">
                    <button
                      aria-label={`Voir le detail de ${user.fullName}`}
                      className="icon-button view"
                      title="Voir le detail"
                      type="button"
                      onClick={() => onInspect(user)}
                    >
                      <EyeIcon />
                    </button>
                    <button
                      aria-label={`Valider le compte de ${user.fullName}`}
                      className="icon-button approve"
                      disabled={loading || user.active || user.archived}
                      title="Valider"
                      type="button"
                      onClick={() => onToggle(user, true)}
                    >
                      <CheckIcon />
                    </button>
                    <button
                      aria-label={`Invalider le compte de ${user.fullName}`}
                      className="icon-button reject"
                      disabled={loading || !user.active || user.archived}
                      title="Invalider"
                      type="button"
                      onClick={() => onToggle(user, false)}
                    >
                      <XIcon />
                    </button>
                    <button
                      aria-label={`${user.archived ? 'Restaurer' : 'Archiver'} le compte de ${user.fullName}`}
                      className="icon-button archive"
                      disabled={loading}
                      title={user.archived ? 'Restaurer' : 'Archiver'}
                      type="button"
                      onClick={() => onArchive(user, !user.archived)}
                    >
                      {user.archived ? <RestoreIcon /> : <ArchiveIcon />}
                    </button>
                  </div>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const bookingStatuses: BookingStatus[] = ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS', 'DONE', 'CANCELLED']
const estimatedHourlyRate = 5000
const monthLabelFormatter = new Intl.DateTimeFormat('fr-FR', { month: 'short' })

function BookingMetricCard({
  icon,
  label,
  value,
}: {
  icon: ReactNode
  label: string
  value: string
}) {
  return (
    <article className="booking-metric-card">
      <div>
        <strong>{value}</strong>
        <span>{label}</span>
      </div>
      <span className="booking-metric-icon">{icon}</span>
    </article>
  )
}

function bookingReference(id: string) {
  return `#BK-${id.replace(/-/g, '').slice(0, 5).toUpperCase()}`
}

function bookingAmount(booking: Booking) {
  return booking.durationHours * estimatedHourlyRate
}

function formatBookingDate(value: string) {
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(new Date(value))
}

function monthKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function parseBookingDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

function BookingsChart({ bookings }: { bookings: Booking[] }) {
  const currentMonth = new Date()
  const months = Array.from({ length: 12 }, (_, index) => {
    const date = new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 11 + index, 1)
    return {
      key: monthKey(date),
      label: monthLabelFormatter.format(date).replace('.', ''),
    }
  })
  const bookingCountsByMonth = bookings.reduce<Record<string, number>>((counts, booking) => {
    const bookingDate = parseBookingDate(booking.serviceDate)
    if (!bookingDate) {
      return counts
    }

    const key = monthKey(bookingDate)
    counts[key] = (counts[key] ?? 0) + 1
    return counts
  }, {})
  const monthlyCounts = months.map((month) => bookingCountsByMonth[month.key] ?? 0)
  const maxCount = Math.max(5, ...monthlyCounts)
  const chartWidth = 720
  const chartHeight = 310
  const padding = { top: 20, right: 26, bottom: 42, left: 38 }
  const plotWidth = chartWidth - padding.left - padding.right
  const plotHeight = chartHeight - padding.top - padding.bottom
  const points = monthlyCounts.map((value, index) => {
    const x = padding.left + (plotWidth / 11) * index
    const y = padding.top + plotHeight - (value / maxCount) * plotHeight
    return { x, y, value }
  })
  const linePoints = points.map((point) => `${point.x},${point.y}`).join(' ')
  const ticks = [maxCount, Math.round(maxCount * 0.75), Math.round(maxCount * 0.5), Math.round(maxCount * 0.25), 0]
  const pieColors = ['#4a919e', '#212e53', '#f7b500', '#66b7a4', '#7c8cf8', '#e38b6d', '#76a9fa', '#9a6dd7', '#3f6c78', '#2f9f74', '#c06f9f', '#7a8794']
  const totalMonthlyBookings = monthlyCounts.reduce((total, value) => total + value, 0)
  const pieRadius = 46
  const pieCircumference = 2 * Math.PI * pieRadius
  let pieOffset = 0
  const pieSegments = months.map((month, index) => {
    const value = monthlyCounts[index]
    const length = totalMonthlyBookings > 0 ? (value / totalMonthlyBookings) * pieCircumference : 0
    const segment = {
      color: pieColors[index % pieColors.length],
      dashArray: `${length} ${pieCircumference - length}`,
      dashOffset: -pieOffset,
      key: month.key,
      label: month.label,
      value,
    }
    pieOffset += length
    return segment
  }).filter((segment) => segment.value > 0)

  return (
    <section className="booking-chart-panel">
      <div className="booking-panel-header">
        <h2>Graphique des réservations</h2>
        <span>Les 12 derniers mois</span>
      </div>
      <div className="booking-pie-summary">
        <svg className="booking-pie-chart" viewBox="0 0 120 120" role="img" aria-label="Diagramme circulaire des reservations sur les 12 derniers mois">
          <circle className="booking-pie-track" cx="60" cy="60" r={pieRadius} />
          {pieSegments.map((segment) => (
            <circle
              className="booking-pie-segment"
              cx="60"
              cy="60"
              key={segment.key}
              r={pieRadius}
              stroke={segment.color}
              strokeDasharray={segment.dashArray}
              strokeDashoffset={segment.dashOffset}
            >
              <title>{`${segment.label}: ${segment.value}`}</title>
            </circle>
          ))}
          <text className="booking-pie-total" x="60" y="56" textAnchor="middle">{totalMonthlyBookings}</text>
          <text className="booking-pie-caption" x="60" y="74" textAnchor="middle">total</text>
        </svg>
        <div className="booking-pie-legend">
          {pieSegments.length === 0 ? (
            <p className="empty-state">Aucune reservation sur la periode.</p>
          ) : (
            pieSegments.map((segment) => (
              <span key={segment.key}>
                <i style={{ backgroundColor: segment.color }} />
                {segment.label}
                <strong>{segment.value}</strong>
              </span>
            ))
          )}
        </div>
      </div>
      <svg className="sales-chart" viewBox={`0 0 ${chartWidth} ${chartHeight}`} role="img" aria-label="Graphique des réservations sur les 12 derniers mois">
        {ticks.map((tick, index) => {
          const y = padding.top + plotHeight - (tick / maxCount) * plotHeight
          return (
            <g key={`${tick}-${index}`}>
              <line x1={padding.left} x2={chartWidth - padding.right} y1={y} y2={y} />
              <text x={padding.left - 12} y={y + 5} textAnchor="end">{tick}</text>
            </g>
          )
        })}
        {months.map((month, index) => {
          const x = padding.left + (plotWidth / 11) * index
          return (
            <g key={month.key}>
              <line className="chart-month-line" x1={x} x2={x} y1={padding.top} y2={padding.top + plotHeight} />
              <text x={x} y={chartHeight - 12} textAnchor="middle">{month.label}</text>
            </g>
          )
        })}
        <polyline points={linePoints} />
        {points.map((point, index) => (
          <circle key={months[index].key} cx={point.x} cy={point.y} r="5">
            <title>{`${months[index].label}: ${point.value}`}</title>
          </circle>
        ))}
      </svg>
    </section>
  )
}

type RecentActivity = {
  id: string
  title: string
  body: string
  meta: string
  timestamp: number
}

function recentActivityDate(value: string) {
  const timestamp = new Date(value).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

function formatActivityDate(timestamp: number) {
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(timestamp))
}

function AdminRecentActivitiesPanel({
  bookings,
  providers,
  users,
}: {
  bookings: Booking[]
  providers: ProviderProfile[]
  users: User[]
}) {
  const [showAll, setShowAll] = useState(false)
  const bookingActivities: RecentActivity[] = bookings.map((booking) => ({
    id: `booking-${booking.id}`,
    title: 'Reservation creee',
    body: `${booking.employerName} a demande ${booking.providerName}.`,
    meta: `${bookingReference(booking.id)} - ${formatStatus(booking.status)}`,
    timestamp: recentActivityDate(booking.createdAt),
  }))
  const providerActivities: RecentActivity[] = providers
    .filter((provider) => provider.status === 'PENDING')
    .map((provider) => ({
      id: `provider-${provider.id}`,
      title: 'Profil prestataire a verifier',
      body: `${provider.fullName} attend une validation de dossier.`,
      meta: provider.zone,
      timestamp: 0,
    }))
  const userActivities: RecentActivity[] = users
    .filter((currentUser) => currentUser.role !== 'ADMIN')
    .slice(0, 4)
    .map((currentUser) => ({
      id: `user-${currentUser.id}`,
      title: 'Compte utilisateur',
      body: `${currentUser.fullName} est inscrit comme ${displayRole(currentUser.role).toLowerCase()}.`,
      meta: currentUser.active ? 'Compte actif' : 'En attente',
      timestamp: 0,
    }))
  const activities = [...bookingActivities, ...providerActivities, ...userActivities]
    .sort((first, second) => second.timestamp - first.timestamp)
  const visibleActivities = showAll ? activities : activities.slice(0, 6)

  return (
    <section className="booking-notifications-panel">
      <div className="booking-panel-header">
        <h2>Activites recentes</h2>
        {activities.length > 6 && (
          <button className="soft-action" type="button" onClick={() => setShowAll((current) => !current)}>
            {showAll ? 'Reduire' : 'Afficher tout'}
          </button>
        )}
      </div>
      {activities.length === 0 && <p className="empty-state">Aucune activite recente.</p>}
      <div className="booking-notification-list">
        {visibleActivities.map((activity) => (
          <article className="booking-notification" key={activity.id}>
            <span className="booking-notification-icon"><ActivityIcon /></span>
            <div>
              <strong>{activity.title}</strong>
              <p>{activity.body}</p>
              <span className="activity-meta">
                {activity.meta}
                {activity.timestamp > 0 ? ` - ${formatActivityDate(activity.timestamp)}` : ''}
              </span>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

function RecentBookingsTable({
  bookings,
  loading,
  onStatusChange,
}: {
  bookings: Booking[]
  loading: boolean
  onStatusChange: (booking: Booking, status: BookingStatus) => void
}) {
  const [showAll, setShowAll] = useState(false)
  const visibleBookings = showAll ? bookings : bookings.slice(0, 6)

  return (
    <section className="recent-bookings-panel">
      <div className="booking-panel-header">
        <h2>Réservations récentes</h2>
        {bookings.length > 6 && (
          <button className="soft-action" type="button" onClick={() => setShowAll((current) => !current)}>
            {showAll ? 'Réduire' : 'Afficher tout'}
          </button>
        )}
      </div>
      {bookings.length === 0 ? (
        <p className="empty-state">Aucune réservation pour le moment.</p>
      ) : (
        <div className="booking-table-wrapper">
          <table className="booking-table">
            <thead>
              <tr>
                <th>Numéro de réservation</th>
                <th>Service</th>
                <th>Date</th>
                <th>Client</th>
                <th>Statut</th>
                <th>Montant</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {visibleBookings.map((booking) => (
                <tr key={booking.id}>
                  <td>{bookingReference(booking.id)}</td>
                  <td>
                    <strong>{booking.providerName}</strong>
                    <span>{booking.address}</span>
                  </td>
                  <td>{formatBookingDate(booking.serviceDate)}</td>
                  <td>{booking.employerName}</td>
                  <td><StatusBadge status={booking.status} /></td>
                  <td>{formatPrice(bookingAmount(booking))}</td>
                  <td>
                    <select
                      aria-label={`Changer le statut de la réservation ${bookingReference(booking.id)}`}
                      disabled={loading}
                      value={booking.status}
                      onChange={(event) => onStatusChange(booking, event.target.value as BookingStatus)}
                    >
                      {bookingStatuses.map((status) => (
                        <option key={status} value={status}>
                          {formatStatus(status)}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function DashboardOverview({
  bookings,
  providers,
  users,
}: {
  bookings: Booking[]
  providers: ProviderProfile[]
  users: User[]
}) {
  return (
    <div className="bookings-dashboard">
      <div className="booking-dashboard-grid">
        <BookingsChart bookings={bookings} />
        <AdminRecentActivitiesPanel bookings={bookings} providers={providers} users={users} />
      </div>
    </div>
  )
}

function BookingsManagement({
  bookings,
  loading,
  onStatusChange,
}: {
  bookings: Booking[]
  loading: boolean
  onStatusChange: (booking: Booking, status: BookingStatus) => void
}) {
  const recentBookings = [...bookings].sort((first, second) => (
    new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime()
  ))

  return (
    <div className="bookings-dashboard">
      <RecentBookingsTable bookings={recentBookings} loading={loading} onStatusChange={onStatusChange} />
    </div>
  )
}

function CreateUserForm({
  loading,
  onCreate,
}: {
  loading: boolean
  onCreate: (payload: AdminCreateUserPayload) => Promise<User>
}) {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('+22177')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Exclude<Role, 'ADMIN'>>('EMPLOYEUR')
  const [error, setError] = useState({ message: '', fields: {} })
  const [successMessage, setSuccessMessage] = useState('')
  const toast = useToast()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError({ message: '', fields: {} })
    setSuccessMessage('')
    try {
      const user = await onCreate({ fullName, email, phone, password, role })
      setFullName('')
      setEmail('')
      setPhone('+22177')
      setPassword('')
      setRole('EMPLOYEUR')
      setSuccessMessage(`Compte cree pour ${user.fullName}.`)
      toast.success('Compte cree', `Le compte ${user.fullName} a ete cree avec le role ${displayRole(user.role)}.`)
    } catch (caughtError) {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Creation du compte impossible', apiError.message)
    }
  }

  return (
    <form className="admin-create-form" onSubmit={handleSubmit}>
      <FormError message={error.message} fields={error.fields} />
      {successMessage && <div className="notice"><strong>Compte cree</strong><span>{successMessage}</span></div>}
      <label>
        Nom complet
        <input value={fullName} onChange={(event) => setFullName(event.target.value)} minLength={5} required />
      </label>
      <label>
        Email
        <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
      </label>
      <label>
        Telephone
        <input
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          pattern="^\+221(70|71|76|77|78)[0-9]{7}$"
          required
        />
      </label>
      <label>
        Mot de passe
        <PasswordInput
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          minLength={6}
          pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{6,}$"
          required
        />
      </label>
      <label>
        Role
        <select value={role} onChange={(event) => setRole(event.target.value as Exclude<Role, 'ADMIN'>)}>
          <option value="EMPLOYEUR">Employeur</option>
          <option value="PRESTATAIRE">Prestataire</option>
        </select>
      </label>
      <button className="button primary" disabled={loading} type="submit">
        Creer le compte
      </button>
    </form>
  )
}

export function AdminDashboard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [activeMenu, setActiveMenu] = useState<AdminMenu>('dashboard')
  const [adminError, setAdminError] = useState({ message: '', fields: {} })
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const usersQuery = useQuery({ queryKey: ['admin', 'users'], queryFn: adminApi.users })
  const providersQuery = useQuery({ queryKey: ['admin', 'providers'], queryFn: () => adminApi.providers() })
  const bookingsQuery = useQuery({ queryKey: ['admin', 'bookings'], queryFn: () => adminApi.bookings() })

  const providerStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; providerName: string; status: ProviderStatus }) => adminApi.updateProviderStatus(id, status),
    onMutate: () => setAdminError({ message: '', fields: {} }),
    onSuccess: (_, variables) => {
      toast.success(
        variables.status === 'APPROVED' ? 'Profil prestataire valide' : 'Profil prestataire rejete',
        `Le profil de ${variables.providerName} a ete ${variables.status === 'APPROVED' ? 'valide' : 'rejete'}.`,
      )
      void queryClient.invalidateQueries({ queryKey: ['admin', 'providers'] })
      void queryClient.invalidateQueries({ queryKey: ['providers'] })
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setAdminError(apiError)
      toast.error('Mise a jour du profil impossible', apiError.message)
    },
  })
  const createUserMutation = useMutation({
    mutationFn: (payload: AdminCreateUserPayload) => adminApi.createUser(payload),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
  const userStatusMutation = useMutation({
    mutationFn: ({ id, active }: { active: boolean; id: string; userName: string }) => adminApi.updateUserActive(id, active),
    onMutate: () => setAdminError({ message: '', fields: {} }),
    onSuccess: (_, variables) => {
      toast.success(
        variables.active ? 'Compte valide' : 'Compte invalide',
        `Le compte de ${variables.userName} a ete ${variables.active ? 'valide' : 'invalide'}.`,
      )
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      void queryClient.invalidateQueries({ queryKey: ['providers'] })
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setAdminError(apiError)
      toast.error('Mise a jour du compte impossible', apiError.message)
    },
  })
  const userArchiveMutation = useMutation({
    mutationFn: ({ id, archived }: { archived: boolean; id: string; userName: string }) => adminApi.updateUserArchived(id, archived),
    onMutate: () => setAdminError({ message: '', fields: {} }),
    onSuccess: (_, variables) => {
      toast.success(
        variables.archived ? 'Compte archive' : 'Compte restaure',
        `Le compte de ${variables.userName} a ete ${variables.archived ? 'archive' : 'restaure'}.`,
      )
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      void queryClient.invalidateQueries({ queryKey: ['providers'] })
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setAdminError(apiError)
      toast.error('Archivage du compte impossible', apiError.message)
    },
  })
  const bookingStatusMutation = useMutation({
    mutationFn: ({ id, status }: { bookingReference: string; id: string; status: BookingStatus }) => adminApi.updateBookingStatus(id, status),
    onSuccess: (_, variables) => {
      toast.success('Reservation mise a jour', `${variables.bookingReference} est maintenant "${formatStatus(variables.status)}".`)
      void queryClient.invalidateQueries({ queryKey: ['admin', 'bookings'] })
    },
    onError: (caughtError) => toast.error('Reservation non mise a jour', getApiError(caughtError).message),
  })

  const manageableUsers = usersQuery.data?.filter((user) => user.role === 'EMPLOYEUR' || user.role === 'PRESTATAIRE') ?? []
  const providers = providersQuery.data ?? []
  const pendingProviderApprovals = countPendingProviderApprovals(manageableUsers, providers)
  const bookings = bookingsQuery.data ?? []
  const pendingBookings = bookings.filter((booking) => booking.status === 'REQUESTED')
  const adminItems: WorkspaceNavItem<AdminMenu>[] = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: <DashboardIcon />,
    },
    {
      id: 'accounts',
      label: 'Gestion des comptes',
      icon: <UsersIcon />,
      badge: manageableUsers.length.toLocaleString('fr-FR'),
    },
    {
      id: 'bookings',
      label: 'Gestion des reservations',
      icon: <BagIcon />,
      badge: pendingBookings.length.toLocaleString('fr-FR'),
    },
  ]

  return (
    <WorkspaceShell
      activeItem={activeMenu}
      eyebrow="Back office"
      items={adminItems}
      title="Pilotage Jaankha"
      userContact={user?.email || user?.phone || undefined}
      userName={user?.fullName || 'Administrateur'}
      onSelect={setActiveMenu}
    >
      {activeMenu === 'dashboard' && (
        <>
      <div className="booking-metrics-grid admin-overview-metrics">
        <BookingMetricCard icon={<UsersIcon />} label="Utilisateurs" value={(usersQuery.data?.length ?? 0).toLocaleString('fr-FR')} />
        <BookingMetricCard icon={<ProfilePendingIcon />} label="Profils en attente" value={pendingProviderApprovals.toLocaleString('fr-FR')} />
        <BookingMetricCard icon={<BagIcon />} label="Réservation totale" value={bookings.length.toLocaleString('fr-FR')} />
        <BookingMetricCard icon={<HourglassIcon />} label="Réservation en attente" value={pendingBookings.length.toLocaleString('fr-FR')} />
      </div>
          <DashboardOverview bookings={bookings} providers={providers} users={usersQuery.data ?? []} />
        </>
      )}
      {usersQuery.isError && <p className="empty-state">Impossible de charger les utilisateurs.</p>}
      {providersQuery.isError && <p className="empty-state">Impossible de charger les profils prestataires.</p>}
      <FormError message={adminError.message} fields={adminError.fields} />
      {activeMenu === 'accounts' && (
        <div className="dashboard-grid">
          <section className="panel span-2">
            <h2>Creer un compte</h2>
            <CreateUserForm loading={createUserMutation.isPending} onCreate={(payload) => createUserMutation.mutateAsync(payload)} />
          </section>
          <section className="panel span-2">
            <h2>Gestion des comptes</h2>
            {manageableUsers.length === 0 ? (
              <p className="empty-state">Aucun compte employeur ou prestataire pour le moment.</p>
            ) : (
              <AccountsTable
                loading={userStatusMutation.isPending || userArchiveMutation.isPending}
                users={manageableUsers}
                onToggle={(user, active) => userStatusMutation.mutate({ id: user.id, active, userName: user.fullName })}
                onArchive={(user, archived) => userArchiveMutation.mutate({ id: user.id, archived, userName: user.fullName })}
                onInspect={setSelectedUser}
              />
            )}
            {selectedUser && <UserDetailPanel user={selectedUser} onClose={() => setSelectedUser(null)} />}
          </section>
          <section className="panel span-2">
            <h2>Profils prestataires a verifier</h2>
            <div className="stack">
              {providersQuery.data?.length === 0 && <p className="empty-state">Aucun profil prestataire pour le moment.</p>}
              {providersQuery.data?.map((provider) => (
                <article className="admin-provider-row" key={provider.id}>
                  <div>
                    <strong>{provider.fullName}</strong>
                  <p>{provider.zone} - {formatPrice(provider.monthlyRate)}/mois - {provider.experienceYears} an(s)</p>
                    <p>{provider.bio || 'Bio non renseignee.'}</p>
                    <div className="chips">
                      {provider.skills.slice(0, 5).map((item) => (
                        <span key={item}>{item}</span>
                      ))}
                    </div>
                  </div>
                  <StatusBadge status={provider.status} />
                  <button
                    aria-label={`Valider le profil de ${provider.fullName}`}
                    className="icon-button approve"
                    title="Valider"
                    type="button"
                    onClick={() => providerStatusMutation.mutate({ id: provider.id, providerName: provider.fullName, status: 'APPROVED' })}
                  >
                    <CheckIcon />
                  </button>
                  <button
                    aria-label={`Rejeter le profil de ${provider.fullName}`}
                    className="icon-button reject"
                    title="Rejeter"
                    type="button"
                    onClick={() => providerStatusMutation.mutate({ id: provider.id, providerName: provider.fullName, status: 'REJECTED' })}
                  >
                    <XIcon />
                  </button>
                </article>
              ))}
            </div>
          </section>
        </div>
      )}
      {activeMenu === 'bookings' && (
        <BookingsManagement
          bookings={bookings}
          loading={bookingStatusMutation.isPending}
          onStatusChange={(booking, status) => bookingStatusMutation.mutate({ id: booking.id, bookingReference: bookingReference(booking.id), status })}
        />
      )}
    </WorkspaceShell>
  )
}
