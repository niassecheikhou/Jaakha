import { Link } from 'react-router-dom'
import { LoginForm } from '../features/auth/AuthForms'

export function LoginPage() {
  return (
    <section className="auth-page">
      <div>
        <p className="eyebrow">Connexion</p>
        <h1>Acceder a Jaankha</h1>
        <p className="muted">Connectez-vous pour reserver, suivre vos missions ou administrer la plateforme.</p>
      </div>
      <LoginForm />
      <p>
        Pas encore de compte ? <Link to="/register">Creer un compte</Link>
      </p>
    </section>
  )
}
