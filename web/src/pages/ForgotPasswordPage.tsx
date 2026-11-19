import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { FormError } from '../components/FormError'
import { useToast } from '../components/toast-context'
import { authApi, getApiError } from '../services/api'

export function ForgotPasswordPage() {
  const toast = useToast()
  const [identifier, setIdentifier] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState({ message: '', fields: {} })
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setIsSubmitting(true)
    setError({ message: '', fields: {} })
    setMessage('')
    try {
      const response = await authApi.forgotPassword({ identifier })
      setMessage(response.message)
      toast.success('Email envoye', response.message)
    } catch (caughtError) {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Demande impossible', apiError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <div>
        <p className="eyebrow">Mot de passe oublie</p>
        <h1>Recevoir un lien de reinitialisation</h1>
        <p className="muted">Indique ton email ou ton telephone. Si le compte existe, un email sera envoye.</p>
      </div>
      <form className="form-panel" onSubmit={handleSubmit}>
        <FormError message={error.message} fields={error.fields} />
        {message && <div className="notice"><strong>Demande recue</strong><span>{message}</span></div>}
        <label>
          Email ou telephone
          <input value={identifier} onChange={(event) => setIdentifier(event.target.value)} required />
        </label>
        <button className="button primary" type="submit" disabled={isSubmitting}>
          Envoyer le lien
        </button>
      </form>
      <p>
        Mot de passe retrouve ? <Link to="/login">Se connecter</Link>
      </p>
    </section>
  )
}
