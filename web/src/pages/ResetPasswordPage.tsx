import { useMemo, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { FormError } from '../components/FormError'
import { PasswordInput } from '../components/PasswordInput'
import { useToast } from '../components/toast-context'
import { authApi, getApiError } from '../services/api'

export function ResetPasswordPage() {
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const token = useMemo(() => searchParams.get('token') ?? '', [searchParams])
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState({ message: '', fields: {} })
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError({ message: '', fields: {} })
    setMessage('')

    if (password !== confirmPassword) {
      setError({ message: 'Les deux mots de passe ne correspondent pas.', fields: {} })
      return
    }

    setIsSubmitting(true)
    try {
      const response = await authApi.resetPassword({ token, password })
      setMessage(response.message)
      setPassword('')
      setConfirmPassword('')
      toast.success('Mot de passe modifie', response.message)
    } catch (caughtError) {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Reinitialisation impossible', apiError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <div>
        <p className="eyebrow">Nouveau mot de passe</p>
        <h1>Reinitialiser mon acces</h1>
        <p className="muted">Choisis un nouveau mot de passe securise pour ton compte Jaankha.</p>
      </div>
      <form className="form-panel" onSubmit={handleSubmit}>
        <FormError message={error.message} fields={error.fields} />
        {!token && <div className="notice"><strong>Lien manquant</strong><span>Demande un nouveau lien de reinitialisation.</span></div>}
        {message && <div className="notice"><strong>Mot de passe modifie</strong><span>{message}</span></div>}
        <label>
          Nouveau mot de passe
          <PasswordInput
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            minLength={6}
            pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{6,}$"
            required
          />
        </label>
        <label>
          Confirmer le mot de passe
          <PasswordInput
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            required
          />
        </label>
        <button className="button primary" type="submit" disabled={isSubmitting || !token}>
          Reinitialiser
        </button>
      </form>
      <p>
        Retour a la connexion ? <Link to="/login">Se connecter</Link>
      </p>
    </section>
  )
}
