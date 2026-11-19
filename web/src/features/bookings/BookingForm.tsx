import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { FormError } from '../../components/FormError'
import { useToast } from '../../components/toast-context'
import { bookingsApi, getApiError } from '../../services/api'

export function BookingForm({ providerId }: { providerId: string }) {
  const navigate = useNavigate()
  const toast = useToast()
  const [error, setError] = useState({ message: '', fields: {} })

  const bookingMutation = useMutation({
    mutationFn: bookingsApi.create,
    onSuccess: () => {
      toast.success('Reservation envoyee', 'Ta demande de reservation a ete transmise au prestataire.')
      navigate('/employer')
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Reservation impossible', apiError.message)
    },
  })

  function handleBooking() {
    setError({ message: '', fields: {} })
    bookingMutation.mutate({ providerId })
  }

  return (
    <div className="booking-action">
      <FormError message={error.message} fields={error.fields} />
      <button className="button primary" type="button" disabled={bookingMutation.isPending} onClick={handleBooking}>
        {bookingMutation.isPending ? 'Reservation en cours...' : 'Reserver'}
      </button>
    </div>
  )
}
