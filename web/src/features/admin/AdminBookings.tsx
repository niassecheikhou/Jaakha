import { useState, type ReactNode } from 'react'
import { ActionsMenu, type ActionsMenuItem } from '../../components/ActionsMenu'
import { StatusBadge } from '../../components/StatusBadge'
import type { Booking, BookingStatus, ProviderProfile, User } from '../../types/api'
import { isWithinPeriod, periodBuckets, periodRange, type AnalysisPeriod } from '../../utils/analysis-period'
import { formatPrice, formatStatus } from '../../utils/format'
import { ActivityIcon, CheckIcon, FlagIcon, PlayIcon, XIcon } from './AdminIcons'
import { bookingReference } from './admin-booking-utils'
import { displayRole } from './admin-utils'

// Cycle de vie impose par le backend (BookingStatusPolicy) : REQUESTED -> ACCEPTED -> IN_PROGRESS -> DONE,
// annulation possible tant que la mission n'est pas terminee. DONE et CANCELLED sont definitifs.
const bookingSteps: { target: BookingStatus; label: string; statusLabel: string; waitingFor: string; icon: ReactNode }[] = [
  { target: 'ACCEPTED', label: 'Accepter la reservation', statusLabel: 'Acceptee', waitingFor: 'Etape 1', icon: <CheckIcon /> },
  { target: 'IN_PROGRESS', label: 'Demarrer la mission', statusLabel: 'En cours', waitingFor: 'Apres acceptation', icon: <PlayIcon /> },
  { target: 'DONE', label: 'Terminer la mission', statusLabel: 'Terminee', waitingFor: 'Apres le demarrage', icon: <FlagIcon /> },
]
const stepOrder: BookingStatus[] = ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS', 'DONE']

// Menu d'une reservation : toutes les etapes restantes du parcours sont visibles ;
// seule l'etape suivante est cliquable, les suivantes sont grisees avec leur condition.
function bookingActions(
  booking: Booking,
  loading: boolean,
  onStatusChange: (booking: Booking, status: BookingStatus) => void,
): ActionsMenuItem[] {
  const currentIndex = stepOrder.indexOf(booking.status)
  const steps: ActionsMenuItem[] = bookingSteps
    .filter((step) => stepOrder.indexOf(step.target) > currentIndex)
    .map((step) => {
      const isNext = stepOrder.indexOf(step.target) === currentIndex + 1
      return {
        id: step.target,
        label: step.label,
        icon: step.icon,
        tone: step.target === 'IN_PROGRESS' ? 'view' : 'approve',
        hint: isNext ? `Nouveau statut : ${step.statusLabel}` : step.waitingFor,
        unavailable: !isNext,
        disabled: loading,
        onSelect: () => onStatusChange(booking, step.target),
      }
    })
  return [
    ...steps,
    {
      id: 'cancel',
      label: 'Annuler la reservation',
      icon: <XIcon />,
      tone: 'reject',
      hint: 'Definitif',
      disabled: loading,
      onSelect: () => {
        if (window.confirm(`Annuler la reservation ${bookingReference(booking.id)} ? Cette action est definitive.`)) {
          onStatusChange(booking, 'CANCELLED')
        }
      },
    },
  ]
}

const estimatedHourlyRate = 5000

export function BookingMetricCard({
  icon,
  label,
  value,
  caption,
}: {
  icon: ReactNode
  label: string
  value: string
  // Periode a laquelle se rapporte le chiffre (ex. "Annee 2026").
  caption?: string
}) {
  return (
    <article className="booking-metric-card">
      <div>
        <strong>{value}</strong>
        <span>{label}</span>
        {caption && <small className="booking-metric-caption">{caption}</small>}
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

// Graphique des reservations sur la periode d'analyse (par date de creation, comme les indicateurs).
function BookingsChart({ bookings, period }: { bookings: Booking[]; period: AnalysisPeriod | null }) {
  const { buckets: months, unit } = periodBuckets(period)
  const monthlyCounts = months.map((bucket) => bookings.filter((booking) => {
    const createdAt = new Date(booking.createdAt)
    return createdAt >= bucket.start && createdAt < bucket.end
  }).length)
  const stepCount = Math.max(1, months.length - 1)
  const maxCount = Math.max(5, ...monthlyCounts)
  const chartWidth = 720
  const chartHeight = 310
  const padding = { top: 20, right: 26, bottom: 42, left: 38 }
  const plotWidth = chartWidth - padding.left - padding.right
  const plotHeight = chartHeight - padding.top - padding.bottom
  const points = monthlyCounts.map((value, index) => {
    const x = padding.left + (plotWidth / stepCount) * index
    const y = padding.top + plotHeight - (value / maxCount) * plotHeight
    return { x, y, value }
  })
  const linePoints = points.map((point) => `${point.x},${point.y}`).join(' ')
  const ticks = [maxCount, Math.round(maxCount * 0.75), Math.round(maxCount * 0.5), Math.round(maxCount * 0.25), 0]
  const pieColors = ['#4a919e', '#212e53', '#f7b500', '#66b7a4', '#7c8cf8', '#e38b6d', '#76a9fa', '#9a6dd7', '#3f6c78', '#2f9f74', '#c06f9f', '#7a8794']
  const totalMonthlyBookings = monthlyCounts.reduce((total, value) => total + value, 0)
  const pieRadius = 46
  const pieCircumference = 2 * Math.PI * pieRadius
  const pieLength = (count: number) => (totalMonthlyBookings > 0 ? (count / totalMonthlyBookings) * pieCircumference : 0)
  const pieSegments = months.map((month, index) => {
    const value = monthlyCounts[index]
    const length = pieLength(value)
    // Chaque part commence la ou s'arretent les precedentes.
    const offset = pieLength(monthlyCounts.slice(0, index).reduce((total, count) => total + count, 0))
    return {
      color: pieColors[index % pieColors.length],
      dashArray: `${length} ${pieCircumference - length}`,
      dashOffset: -offset,
      key: month.key,
      label: month.label,
      value,
    }
  }).filter((segment) => segment.value > 0)

  return (
    <section className="booking-chart-panel">
      <div className="booking-panel-header">
        <h2>Graphique des reservations</h2>
        <span>{unit}</span>
      </div>
      <div className="booking-pie-summary">
        <svg className="booking-pie-chart" viewBox="0 0 120 120" role="img" aria-label={`Diagramme circulaire des reservations : ${unit}`}>
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
      <svg className="sales-chart" viewBox={`0 0 ${chartWidth} ${chartHeight}`} role="img" aria-label={`Graphique des reservations : ${unit}`}>
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
          const x = padding.left + (plotWidth / stepCount) * index
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
  period,
}: {
  bookings: Booking[]
  providers: ProviderProfile[]
  users: User[]
  period: AnalysisPeriod | null
}) {
  const [showAll, setShowAll] = useState(false)
  const range = period ? periodRange(period) : null
  // Date d'inscription de chaque compte : sert aussi a dater les profils prestataires a verifier.
  const registrationByUserId = new Map(users.map((account) => [account.id, account.createdAt]))
  const bookingActivities: RecentActivity[] = bookings
    .filter((booking) => isWithinPeriod(booking.createdAt, range))
    .map((booking) => ({
      id: `booking-${booking.id}`,
      title: 'Reservation creee',
      body: `${booking.employerName} a demande ${booking.providerName}.`,
      meta: `${bookingReference(booking.id)} - ${formatStatus(booking.status)}`,
      timestamp: recentActivityDate(booking.createdAt),
    }))
  const providerActivities: RecentActivity[] = providers
    .filter((provider) => provider.status === 'PENDING')
    .filter((provider) => isWithinPeriod(registrationByUserId.get(provider.userId), range))
    .map((provider) => ({
      id: `provider-${provider.id}`,
      title: 'Profil prestataire a verifier',
      body: `${provider.fullName} attend une validation de dossier.`,
      meta: provider.zone,
      timestamp: recentActivityDate(registrationByUserId.get(provider.userId) ?? ''),
    }))
  const userActivities: RecentActivity[] = users
    .filter((currentUser) => currentUser.role !== 'ADMIN')
    .filter((currentUser) => isWithinPeriod(currentUser.createdAt, range))
    .map((currentUser) => ({
      id: `user-${currentUser.id}`,
      title: 'Compte utilisateur',
      body: `${currentUser.fullName} est inscrit comme ${displayRole(currentUser.role).toLowerCase()}.`,
      meta: currentUser.active ? 'Compte actif' : 'En attente',
      timestamp: recentActivityDate(currentUser.createdAt ?? ''),
    }))
  const activities = [...bookingActivities, ...providerActivities, ...userActivities]
    .sort((first, second) => second.timestamp - first.timestamp)
  const visibleActivities = showAll ? activities : activities.slice(0, 6)

  return (
    <section className="booking-notifications-panel">
      <div className="booking-panel-header">
        <div>
          <h2>Activites recentes</h2>
          <span>{range ? range.label : 'Depuis le debut'}</span>
        </div>
        {activities.length > 6 && (
          <button className="soft-action" type="button" onClick={() => setShowAll((current) => !current)}>
            {showAll ? 'Reduire' : 'Afficher tout'}
          </button>
        )}
      </div>
      {activities.length === 0 && (
        <p className="empty-state">{range ? `Aucune activite sur la periode (${range.label}).` : 'Aucune activite recente.'}</p>
      )}
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
  period,
}: {
  bookings: Booking[]
  providers: ProviderProfile[]
  users: User[]
  // Periode d'analyse choisie dans la barre de filtres (null : sans filtre).
  period: AnalysisPeriod | null
}) {
  return (
    <div className="bookings-dashboard">
      <div className="booking-dashboard-grid">
        <BookingsChart bookings={bookings} period={period} />
        <AdminRecentActivitiesPanel bookings={bookings} providers={providers} users={users} period={period} />
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
