import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { FormError } from '../../components/FormError'
import { useToast } from '../../components/toast-context'
import { getApiError, reviewsApi } from '../../services/api'

export function ReviewForm({ bookingId }: { bookingId: string }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [error, setError] = useState({ message: '', fields: {} })
  const [successMessage, setSuccessMessage] = useState('')

  const reviewMutation = useMutation({
    mutationFn: reviewsApi.create,
    onSuccess: () => {
      setComment('')
      setSuccessMessage('Avis enregistre.')
      toast.success('Avis enregistre', 'Merci, ton avis a ete publie avec succes.')
      void queryClient.invalidateQueries({ queryKey: ['bookings', 'me'] })
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setSuccessMessage('')
      setError(apiError)
      toast.error('Avis non enregistre', apiError.message)
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError({ message: '', fields: {} })
    setSuccessMessage('')
    reviewMutation.mutate({ bookingId, rating, comment })
  }

  return (
    <form className="inline-form" onSubmit={handleSubmit}>
      <FormError message={error.message} fields={error.fields} />
      {successMessage && <p className="notice success">{successMessage}</p>}
      <select value={rating} onChange={(event) => setRating(Number(event.target.value))}>
        {[5, 4, 3, 2, 1].map((value) => (
          <option key={value} value={value}>
            {value}/5
          </option>
        ))}
      </select>
      <input placeholder="Votre avis" value={comment} onChange={(event) => setComment(event.target.value)} />
      <button className="button secondary" type="submit" disabled={reviewMutation.isPending}>
        Noter
      </button>
    </form>
  )
}
