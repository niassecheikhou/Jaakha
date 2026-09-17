import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <section className="auth-page">
      <div>
        <p className="eyebrow">404</p>
        <h1>Page introuvable</h1>
      </div>
      <Link className="button primary" to="/">
        Retour à l'accueil
      </Link>
    </section> 
  )
}
