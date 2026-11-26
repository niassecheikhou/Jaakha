import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { FormError } from '../../components/FormError'
import { useToast } from '../../components/toast-context'
import { getApiError, providersApi } from '../../services/api'
import type { AdminBookingPayload, Booking, User } from '../../types/api'
import { formatPrice } from '../../utils/format'

function todayIso() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

// Reservation faite par l'administrateur pour un employeur qu'il gere (comptes valides uniquement),
// avec un prestataire disponible ; elle peut etre validee des sa creation.
export function AdminBookingForm({
  employers,
  loading,
  onCreate,
}: {
  employers: User[]
  loading: boolean
  onCreate: (payload: AdminBookingPayload) => Promise<Booking>
}) {
  const toast = useToast()
  const providersQuery = useQuery({ queryKey: ['providers', 'bookable'], queryFn: () => providersApi.search({}) })
  const providers = providersQuery.data ?? []
  const bookableEmployers = employers
    .filter((account) => account.role === 'EMPLOYEUR' && account.active && !account.archived)
    .sort((first, second) => first.fullName.localeCompare(second.fullName, 'fr'))

  const [employerId, setEmployerId] = useState('')
  const [providerId, setProviderId] = useState('')
  const [serviceDate, setServiceDate] = useState(todayIso)
  const [startTime, setStartTime] = useState('09:00')
  const [durationHours, setDurationHours] = useState(2)
  const [address, setAddress] = useState('')
  const [instructions, setInstructions] = useState('')
  const [validate, setValidate] = useState(true)
  const [error, setError] = useState({ message: '', fields: {} })

  function selectEmployer(id: string) {
    setEmployerId(id)
    // Proposition d'adresse : celle du dossier de l'employeur, sinon sa zone.
    const employer = bookableEmployers.find((account) => account.id === id)
    if (employer && !address) {
      setAddress(employer.address || employer.zone || '')
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError({ message: '', fields: {} })
    try {
      const booking = await onCreate({ employerId, providerId, serviceDate, startTime, durationHours, address, instructions, validate })
      toast.success(
        validate ? 'Reservation creee et validee' : 'Reservation creee',
        `${booking.employerName} avec ${booking.providerName}, le ${new Date(booking.serviceDate).toLocaleDateString('fr-FR')}.`,
      )
      setProviderId('')
      setInstructions('')
    } catch (caughtError) {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Reservation impossible', apiError.message)
    }
  }

  return (
    <section className="panel">
      <h2>Nouvelle reservation</h2>
      <p className="muted">Reserve un prestataire pour le compte d'un employeur que tu geres (compte valide).</p>
      {bookableEmployers.length === 0 ? (
        <p className="empty-state">Aucun employeur valide parmi tes comptes : valide d'abord le compte d'un employeur.</p>
      ) : (
        <form className="admin-create-form" onSubmit={handleSubmit}>
          <FormError message={error.message} fields={error.fields} />
          <label>
            Employeur
            <select value={employerId} onChange={(event) => selectEmployer(event.target.value)} required>
              <option value="">Choisir un employeur</option>
              {bookableEmployers.map((account) => (
                <option key={account.id} value={account.id}>{account.fullName}</option>
              ))}
            </select>
          </label>
          <label>
            Prestataire
            <select value={providerId} onChange={(event) => setProviderId(event.target.value)} required>
              <option value="">{providersQuery.isLoading ? 'Chargement...' : 'Choisir un prestataire'}</option>
              {providers.map((provider) => (
                <option key={provider.id} value={provider.id}>
                  {provider.fullName} - {provider.zone} - {formatPrice(provider.monthlyRate)}/mois
                </option>
              ))}
            </select>
          </label>
          <label>
            Date
            <input type="date" min={todayIso()} value={serviceDate} onChange={(event) => setServiceDate(event.target.value)} required />
          </label>
          <label>
            Heure de debut
            <input type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} required />
          </label>
          <label>
            Duree (heures)
            <input type="number" min={1} max={12} value={durationHours} onChange={(event) => setDurationHours(Number(event.target.value))} required />
          </label>
          <label>
            Adresse de la prestation
            <input value={address} maxLength={255} onChange={(event) => setAddress(event.target.value)} required />
          </label>
          <label className="span-2">
            Instructions (facultatif)
            <input value={instructions} maxLength={1000} onChange={(event) => setInstructions(event.target.value)} />
          </label>
          <label className="checkbox-row">
            <input type="checkbox" checked={validate} onChange={(event) => setValidate(event.target.checked)} />
            Valider la reservation des sa creation
          </label>
          <button className="button primary" disabled={loading || providers.length === 0} type="submit">
            {validate ? 'Creer et valider' : 'Creer la reservation'}
          </button>
        </form>
      )}
    </section>
  )
}
