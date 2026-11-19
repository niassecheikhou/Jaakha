import { useState, type ReactNode } from 'react'
import { ActionsMenu, type ActionsMenuItem } from '../../components/ActionsMenu'
import { StatusBadge } from '../../components/StatusBadge'
import type { Booking, BookingStatus, ProviderProfile, User } from '../../types/api'
import { formatPrice, formatStatus } from '../../utils/format'
import { ActivityIcon, CheckIcon, FlagIcon, PlayIcon, XIcon } from './AdminIcons'
import { bookingReference } from './admin-booking-utils'
import { displayRole } from './admin-utils'

// Actions possibles selon le statut, alignees sur le cycle impose par le backend (BookingStatusPolicy) :
// REQUESTED -> ACCEPTED -> IN_PROGRESS -> DONE, annulation possible tant que la mission n'est pas terminee.
// DONE et CANCELLED sont definitifs : aucune action.
function bookingActions(
  booking: Booking,
  loading: boolean,
  onStatusChange: (booking: Booking, status: BookingStatus) => void,
): ActionsMenuItem[] {
  const change = (status: BookingStatus) => () => onStatusChange(booking, status)
  const cancel: ActionsMenuItem = {
    id: 'cancel',
    label: 'Annuler la reservation',
    icon: <XIcon />,
    tone: 'reject',
    disabled: loading,
    onSelect: () => {
      if (window.confirm(`Annuler la reservation ${bookingReference(booking.id)} ? Cette action est definitive.`)) {
        onStatusChange(booking, 'CANCELLED')
      }
    },
  }
  switch (booking.status) {
    case 'REQUESTED':
      return [
        { id: 'accept', label: 'Accepter la reservation', icon: <CheckIcon />, tone: 'approve', disabled: loading, onSelect: change('ACCEPTED') },
        cancel,
      ]
    case 'ACCEPTED':
      return [
        { id: 'start', label: 'Demarrer la mission', icon: <PlayIcon />, tone: 'view', disabled: loading, onSelect: change('IN_PROGRESS') },
        cancel,
      ]
    case 'IN_PROGRESS':
      return [
        { id: 'done', label: 'Marquer comme terminee', icon: <FlagIcon />, tone: 'approve', disabled: loading, onSelect: change('DONE') },
        cancel,
      ]
    default:
      return []
  }
}
const estimatedHourlyRate = 5000
const monthLabelFormatter = new Intl.DateTimeFormat('fr-FR', { month: 'short' })

export function BookingMetricCard({
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
        <h2>Graphique des reservations</h2>
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
      <svg className="sales-chart" viewBox={`0 0 ${chartWidth} ${chartHeight}`} role="img" aria-label="Graphique des reservations sur les 12 derniers mois">
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
        <h2>Reservations recentes</h2>
        {bookings.length > 6 && (
          <button className="soft-action" type="button" onClick={() => setShowAll((current) => !current)}>
            {showAll ? 'Reduire' : 'Afficher tout'}
          </button>
        )}
      </div>
      {bookings.length === 0 ? (
        <p className="empty-state">Aucune reservation pour le moment.</p>
      ) : (
        <div className="booking-table-wrapper">
          <table className="booking-table">
            <thead>
              <tr>
                <th>Numero de reservation</th>
                <th>Service</th>
                <th>Date</th>
                <th>Client</th>
                <th>Statut</th>
                <th>Montant</th>
                <th className="actions-column">Actions</th>
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
                  <td className="actions-column">
                    {booking.status === 'DONE' || booking.status === 'CANCELLED' ? (
                      <span className="muted">Cloturee</span>
                    ) : (
                      <ActionsMenu
                        subject={`la reservation ${bookingReference(booking.id)}`}
                        items={bookingActions(booking, loading, onStatusChange)}
                      />
                    )}
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

export function DashboardOverview({
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

export function BookingsManagement({
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
