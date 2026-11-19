import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { StarRating } from '../../../components/StarRating'
import { providersApi } from '../../../services/api'
import type { DayOfWeek, ProviderProfile } from '../../../types/api'
import { formatDay, formatPrice } from '../../../utils/format'

const days: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
]

const serviceVisuals = [
  {
    className: 'cleaning',
    image:
      'https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=900&q=80',
    label: 'Services maison',
    title: 'Nettoyage de maison',
    keywords: ['menage', 'nettoyage', 'sol', 'miroir', 'vitre', 'maison'],
  },
  {
    className: 'cooking',
    image:
      'https://images.unsplash.com/photo-1556911220-bff31c812dba?auto=format&fit=crop&w=900&q=80',
    label: 'Cuisine domicile',
    title: 'Cuisine familiale',
    keywords: ['cuisine', 'cuisinier', 'repas', 'traiteur'],
  },
  {
    className: 'childcare',
    image:
      'https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?auto=format&fit=crop&w=900&q=80',
    label: 'Garde enfant',
    title: 'Aide familiale',
    keywords: ['nounou', 'enfant', 'garde', 'baby', 'famille'],
  },
  {
    className: 'beauty',
    image:
      'https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&w=900&q=80',
    label: 'Beaute domicile',
    title: 'Coiffure et soins',
    keywords: ['coiffure', 'beaute', 'maquillage', 'ongle', 'soin'],
  },
  {
    className: 'tailoring',
    image:
      'https://images.unsplash.com/photo-1517840933437-c41356892b35?auto=format&fit=crop&w=900&q=80',
    label: 'Couture',
    title: 'Retouche et confection',
    keywords: ['couture', 'tailleur', 'retouche', 'confection', 'tissu'],
  },
  {
    className: 'repair',
    image:
      'https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=900&q=80',
    label: 'Depannage',
    title: 'Reparation maison',
    keywords: ['bricolage', 'plomberie', 'electricite', 'reparation', 'jardinage', 'gardiennage'],
  },
]

function normalize(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
}

function getProviderVisual(provider: ProviderProfile) {
  const searchable = normalize(
    [provider.fullName, provider.zone, provider.bio, ...provider.skills].filter(Boolean).join(' '),
  )

  return serviceVisuals.find((visual) => visual.keywords.some((keyword) => searchable.includes(keyword))) ?? serviceVisuals[0]
}

function formatServiceName(provider: ProviderProfile, fallback: string) {
  const skill = provider.skills.find((item) => item.trim().length > 0)

  if (!skill) {
    return fallback
  }

  return skill
    .trim()
    .split(/\s+/)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ')
}

export function ProviderSearch() {
  const [zone, setZone] = useState('')
  const [skill, setSkill] = useState('')
  const [maxMonthlyRate, setMaxMonthlyRate] = useState('')
  const [availableDay, setAvailableDay] = useState('')

  const params = useMemo(() => {
    return Object.fromEntries(
      Object.entries({ zone, skill, maxPrice: maxMonthlyRate, availableDay }).filter(([, value]) => value),
    )
  }, [availableDay, maxMonthlyRate, skill, zone])

  const providersQuery = useQuery({
    queryKey: ['providers', params],
    queryFn: () => providersApi.search(params),
  })
  const visibleProviders = providersQuery.data?.filter((provider) => provider.status === 'APPROVED') ?? []

  return (
    <section className="page-section">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Dakar</p>
          <h1>Trouver un prestataire fiable</h1>
        </div>
      </div>

      <form className="filters">
        <input placeholder="Zone" value={zone} onChange={(event) => setZone(event.target.value)} />
        <input placeholder="Competence" value={skill} onChange={(event) => setSkill(event.target.value)} />
        <input
          min="0"
          placeholder="Tarif mensuel max"
          type="number"
          value={maxMonthlyRate}
          onChange={(event) => setMaxMonthlyRate(event.target.value)}
        />
        <select value={availableDay} onChange={(event) => setAvailableDay(event.target.value)}>
          <option value="">Tous les jours</option>
          {days.map((day) => (
            <option key={day} value={day}>
              {formatDay(day)}
            </option>
          ))}
        </select>
      </form>

      {providersQuery.isLoading && <p>Chargement des prestataires...</p>}
      {providersQuery.isError && <p className="empty-state">Impossible de charger les prestataires.</p>}
      {providersQuery.isSuccess && visibleProviders.length === 0 && <p className="empty-state">Aucun prestataire valide pour ces filtres.</p>}

      <div className="provider-grid">
        {visibleProviders.map((provider) => {
          const visual = getProviderVisual(provider)
          const serviceName = formatServiceName(provider, visual.title)

          return (
            <article className={`provider-card provider-service-card ${visual.className}`} key={provider.id}>
              <div
                aria-label={visual.title}
                className="provider-service-image"
                role="img"
                style={{ backgroundImage: `url("${visual.image}")` }}
              />
              <div className="provider-service-meta">
                <span>{formatPrice(provider.monthlyRate)}/mois</span>
                <span className="provider-rating-pill">
                  <StarRating rating={provider.averageRating || 0} />
                </span>
              </div>
              <div className="provider-service-content">
                <span>{visual.label}</span>
                <h2>{serviceName}</h2>
                <p>
                  {provider.fullName} · {provider.zone}
                </p>
              </div>
              <Link aria-label={`Voir le profil de ${provider.fullName}`} className="provider-service-action" to={`/providers/${provider.id}`}>
                →
              </Link>
            </article>
          )
        })}
      </div>
    </section>
  )
}
