import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { FormError } from '../../components/FormError'
import { PasswordInput } from '../../components/PasswordInput'
import { useToast } from '../../components/toast-context'
import { useAuth } from '../../hooks/use-auth'
import { getApiError } from '../../services/api'

export function LoginForm() {
  const { login } = useAuth()
  const toast = useToast()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState({ message: '', fields: {} })
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setIsSubmitting(true)
    setError({ message: '', fields: {} })
    try {
      await login({ identifier, password })
      toast.success('Connexion reussie', 'Bienvenue, ta session est ouverte.')
    } catch (caughtError) {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Connexion impossible', apiError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="form-panel" onSubmit={handleSubmit}>
      <FormError message={error.message} fields={error.fields} />
      <label>
        Email ou telephone
        <input value={identifier} onChange={(event) => setIdentifier(event.target.value)} required />
      </label>
      <label>
        Mot de passe
        <PasswordInput
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
      </label>
      <button className="button primary" type="submit" disabled={isSubmitting}>
        Se connecter
      </button>
      <Link className="auth-inline-link" to="/forgot-password">
        Mot de passe oublie ?
      </Link>
    </form>
  )
}
