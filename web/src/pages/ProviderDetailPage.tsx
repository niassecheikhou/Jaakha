import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { StarRating } from '../components/StarRating'
import { BookingForm } from '../features/bookings/BookingForm'
import { providersApi } from '../services/api'
import { useAuth } from '../hooks/use-auth'
import { formatDay, formatPrice } from '../utils/format'

export function ProviderDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated, user } = useAuth()
  const providerQuery = useQuery({
    queryKey: ['providers', id],
    queryFn: () => providersApi.detail(id ?? ''),
    enabled: Boolean(id),
  })
  const reviewsQuery = useQuery({
    queryKey: ['providers', id, 'reviews'],
    queryFn: () => providersApi.reviews(id ?? ''),
    enabled: Boolean(id),
  })

  if (providerQuery.isLoading) {
    return <p>Chargement du profil...</p>
  }

  if (!providerQuery.data || !id) {
    return <p className="empty-state">Prestataire introuvable.</p>
  }

  const provider = providerQuery.data
  const canBook = isAuthenticated && user?.role === 'EMPLOYEUR'

  return (
    <section className="page-section detail-grid">
      <article className="profile-detail">
        <div className="avatar large">{provider.fullName.slice(0, 1).toUpperCase()}</div>
        <div>
          <p className="eyebrow">{provider.zone}</p>
          <h1>{provider.fullName}</h1>
          <p className="muted">{provider.bio || 'Ce prestataire est disponible pour des missions a Dakar.'}</p>
        </div>
        <div className="metrics">
          <div><strong>{formatPrice(provider.monthlyRate)}</strong><span>Par mois</span></div>
          <div><strong><StarRating rating={provider.averageRating || 0} /></strong><span>Note moyenne</span></div>
          <div><strong>{provider.experienceYears}</strong><span>Annees exp.</span></div>
        </div>
        <div className="chips">
          {provider.skills.map((skill) => (
            <span key={skill}>{skill}</span>
          ))}
        </div>
        <div className="availability-list">
          {provider.availability.map((slot) => (
            <span key={`${slot.dayOfWeek}-${slot.startTime}`}>
              {formatDay(slot.dayOfWeek)} {slot.startTime}-{slot.endTime}
            </span>
          ))}
        </div>
        <Link className="button secondary" to="/">
          Retour
        </Link>
      </article>
      <aside className="panel">
        <h2>Reservation</h2>
        {canBook && <BookingForm providerId={id} />}
        {!isAuthenticated && (
          <button className="button primary" type="button" onClick={() => navigate('/login')}>
            Se connecter pour reserver
          </button>
        )}
        {isAuthenticated && user?.role !== 'EMPLOYEUR' && (
          <p className="empty-state">Seuls les employeurs peuvent creer une reservation.</p>
        )}
      </aside>
      <section className="panel span-2">
        <h2>Avis</h2>
        <div className="stack">
          {reviewsQuery.data?.length === 0 && <p className="empty-state">Aucun avis pour le moment.</p>}
          {reviewsQuery.data?.map((review) => (
            <article className="notice" key={review.id}>
              <strong className="review-heading"><StarRating rating={review.rating} /> {review.employerName}</strong>
              <p>{review.comment || 'Avis sans commentaire.'}</p>
            </article>
          ))}
        </div>
      </section>
    </section>
  )
}
