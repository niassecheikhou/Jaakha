import { StatusBadge } from '../../components/StatusBadge'
import type { ProviderProfile, ProviderStatus } from '../../types/api'
import { formatPrice } from '../../utils/format'
import { ActionsMenu, type ActionsMenuItem } from '../../components/ActionsMenu'
import { CheckIcon, EyeIcon, XIcon } from './AdminIcons'

// Actions du profil public : on ne propose pas de revalider un profil deja valide (ni l'inverse).
function providerActions(
  provider: ProviderProfile,
  onInspect: (provider: ProviderProfile) => void,
  onStatusChange: (provider: ProviderProfile, status: ProviderStatus) => void,
): ActionsMenuItem[] {
  const items: ActionsMenuItem[] = [
    { id: 'view', label: "Voir le dossier (piece d'identite)", icon: <EyeIcon />, tone: 'view', onSelect: () => onInspect(provider) },
  ]
  if (provider.status !== 'APPROVED') {
    items.push({ id: 'approve', label: 'Valider le profil', icon: <CheckIcon />, tone: 'approve', onSelect: () => onStatusChange(provider, 'APPROVED') })
  }
  if (provider.status !== 'REJECTED') {
    items.push({ id: 'reject', label: 'Rejeter le profil', icon: <XIcon />, tone: 'reject', onSelect: () => onStatusChange(provider, 'REJECTED') })
  }
  return items
}

export function ProviderProfilesReview({
  providers,
  onStatusChange,
  onInspect,
}: {
  providers: ProviderProfile[]
  onStatusChange: (provider: ProviderProfile, status: ProviderStatus) => void
  onInspect: (provider: ProviderProfile) => void
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
            <ActionsMenu subject={provider.fullName} items={providerActions(provider, onInspect, onStatusChange)} />
          </article>
        ))}
      </div>
    </section>
  )
}
