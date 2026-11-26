import { useState, type FormEvent } from 'react'
import { FormError } from '../../components/FormError'
import { PasswordInput } from '../../components/PasswordInput'
import { useToast } from '../../components/toast-context'
import { getApiError } from '../../services/api'
import type { AdminCreateUserPayload, CreatableRole, User } from '../../types/api'
import { displayRole } from './admin-utils'

// allowAdmin : le super administrateur peut aussi creer des comptes administrateur.
export function CreateUserForm({
  allowAdmin = false,
  loading,
  onCreate,
}: {
  allowAdmin?: boolean
  loading: boolean
  onCreate: (payload: AdminCreateUserPayload) => Promise<User>
}) {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('+22177')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<CreatableRole>('EMPLOYEUR')
  const [error, setError] = useState({ message: '', fields: {} })
  const [successMessage, setSuccessMessage] = useState('')
  const toast = useToast()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError({ message: '', fields: {} })
    setSuccessMessage('')
    try {
      const user = await onCreate({ fullName, email, phone, password, role })
      setFullName('')
      setEmail('')
      setPhone('+22177')
      setPassword('')
      setRole('EMPLOYEUR')
      setSuccessMessage(`Compte cree pour ${user.fullName}.`)
      toast.success('Compte cree', `Le compte ${user.fullName} a ete cree avec le role ${displayRole(user.role)}.`)
    } catch (caughtError) {
      const apiError = getApiError(caughtError)
      setError(apiError)
      toast.error('Creation du compte impossible', apiError.message)
    }
  }

  return (
    <form className="admin-create-form" onSubmit={handleSubmit}>
      <FormError message={error.message} fields={error.fields} />
      {successMessage && <div className="notice"><strong>Compte cree</strong><span>{successMessage}</span></div>}
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
          required
        />
      </label>
      <label>
        Role
        <select value={role} onChange={(event) => setRole(event.target.value as CreatableRole)}>
          <option value="EMPLOYEUR">Employeur</option>
          <option value="PRESTATAIRE">Prestataire</option>
          {allowAdmin && <option value="ADMIN">Administrateur</option>}
        </select>
      </label>
      <button className="button primary" disabled={loading} type="submit">
        Creer le compte
      </button>
    </form>
  )
}
