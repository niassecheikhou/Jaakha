import { useState, type FormEvent } from 'react'
import { FormError } from '../../components/FormError'
import { PasswordInput } from '../../components/PasswordInput'
import { useToast } from '../../components/toast-context'
import { useAuth } from '../../hooks/use-auth'
import { getApiError } from '../../services/api'
import type { UserRole } from '../../types/api'

export function RegisterForm() {
  const { register } = useAuth()
  const toast = useToast()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('+22177')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<UserRole>('EMPLOYEUR')
  const [error, setError] = useState({ message: '', fields: {} })
  const [successMessage, setSuccessMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setIsSubmitting(true)
    setError({ message: '', fields: {} })
    setSuccessMessage('')
    try {
      const user = await register({ fullName, email, phone, password, role })
      toast.success('Compte cree', `Le compte de ${user.fullName} a ete cree avec succes.`)
      if (!user.active) {
        setSuccessMessage("Compte cree avec succes. Complete ton dossier dans le dashboard pour permettre la validation par l'administration.")
      }
    } catch (caughtError) {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Inscription impossible', apiError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="form-panel" onSubmit={handleSubmit}>
      <FormError message={error.message} fields={error.fields} />
      {successMessage && <div className="notice"><strong>Validation requise</strong><span>{successMessage}</span></div>}
      <label>
        Nom complet
        <input value={fullName} onChange={(event) => setFullName(event.target.value)} minLength={5} required />
      </label>
      <label>
        Email
        <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
      </label>
      <label>
        Telephone
        <input
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          pattern="^\+221(70|71|76|77|78)[0-9]{7}$"
          required
        />
      </label>
      <label>
        Mot de passe
        <PasswordInput
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          minLength={6}
          pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{6,}$"
          title="Au moins 6 caracteres, une majuscule, une minuscule, un chiffre et un caractere special."
          required
        />
      </label>
      <label>
        Type de compte
        <select value={role} onChange={(event) => setRole(event.target.value as UserRole)}>
          <option value="EMPLOYEUR">Employeur</option>
          <option value="PRESTATAIRE">Prestataire</option>
        </select>
      </label>
      <button className="button primary" type="submit" disabled={isSubmitting}>
        Creer mon compte
      </button>
    </form>
  )
}
