import { StatusBadge } from '../../components/StatusBadge'
import type { ProviderProfile, ProviderStatus } from '../../types/api'
import { formatPrice } from '../../utils/format'
import { CheckIcon, XIcon } from './AdminIcons'

export function ProviderProfilesReview({
  providers,
  onStatusChange,
}: {
  providers: ProviderProfile[]
  onStatusChange: (provider: ProviderProfile, status: ProviderStatus) => void
}) {
  return (
    <section className="panel span-2">
      <h2>Profils prestataires a verifier</h2>
      <div className="stack">
        {providers.length === 0 && <p className="empty-state">Aucun profil prestataire pour le moment.</p>}
        {providers.map((provider) => (
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
              onClick={() => onStatusChange(provider, 'APPROVED')}
            >
              <CheckIcon />
            </button>
            <button
              aria-label={`Rejeter le profil de ${provider.fullName}`}
              className="icon-button reject"
              title="Rejeter"
              type="button"
              onClick={() => onStatusChange(provider, 'REJECTED')}
            >
              <XIcon />
            </button>
          </article>
        ))}
      </div>
    </section>
  )
}
