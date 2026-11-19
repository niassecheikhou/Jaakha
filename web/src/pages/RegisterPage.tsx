import { Link } from 'react-router-dom'
import { RegisterForm } from '../features/auth/RegisterForm'

export function RegisterPage() {
  return (
    <section className="auth-page">
      <div>
        <p className="eyebrow">Inscription</p>
        <h1>Creer un compte</h1>
        <p className="muted">Un email valide et un numero senegalais au format +221 sont requis.</p>
      </div>
      <RegisterForm />
      <p>
        Deja inscrit ? <Link to="/login">Se connecter</Link>
      </p>
    </section>
  )
}
