import { Link } from 'react-router-dom'
import servicesHero from '../assets/slide2.jpg'

const services = [
  {
    code: 'JOB',
    title: "Offres d'emplois aux femmes de menage",
    body: 'Jaankha aide les femmes de menage a gagner en visibilite, recevoir des demandes et construire une reputation local.',
    actionLabel: 'Creer un profil',
    href: '/register',
  },
  {
    code: 'DOM',
    title: 'Reservation journaliere ou mensuelle',
    body: 'Les employeurs peuvent reserver une femme de menage selon leur besoin: intervention ponctuelle, suivi regulier ou accompagnement mensuel.',
    actionLabel: 'Voir les prestataires',
    href: '/providers',
  },
  {
    code: 'BUR',
    title: 'Nettoyage de bureaux et surfaces commerciales',
    body: 'Des prestations adaptees aux espaces professionnels, commerces, agences et bureaux qui ont besoin de proprete constante.',
    actionLabel: 'Reference service',
    href: '#',
    external: true,
  },
  {
    code: 'IMM',
    title: "Nettoyage des parties communes d'immeuble",
    body: 'Organisation de prestations pour halls, escaliers, couloirs, paliers et zones partagees dans les immeubles.',
    actionLabel: 'Reference service',
    href: '#',
    external: true,
  },
  {
    code: '4D',
    title: 'Desinsectisation - Desinfection - Deratisation - Dereptilisation',
    body: 'Des services specialises pour assainir les lieux, proteger les occupants et traiter les nuisibles de maniere encadree.',
    actionLabel: 'Reference service',
    href: '#',
    external: true,
  },
]

const values = [
  {
    title: 'Confiance',
    body: 'Des profils lisibles, des comptes verifies et des statuts clairs pour rassurer chaque employeur.',
  },
  {
    title: 'Dignite',
    body: 'Valoriser les femmes de menage et les prestataires en donnant plus de visibilite a leur travail.',
  },
  {
    title: 'Regularite',
    body: 'Faciliter les missions recurrentes, les reservations mensuelles et le suivi dans la duree.',
  },
  {
    title: 'Proximite',
    body: 'Mettre en avant des services adaptes aux besoins quotidiens des familles, immeubles et entreprises a Dakar.',
  },
]

const missions = [
  'Connecter les employeurs a des prestataires fiables pour les besoins domestiques et professionnels.',
  'Creer des opportunites de travail visibles et structurees pour les femmes de menage.',
  'Simplifier la reservation, le suivi et la validation des prestations.',
]

export function ServicesPage() {
  return (
    <div className="services-page">
      <section className="services-hero">
        <div className="services-hero-copy">
          <p className="eyebrow">Nos services</p>
          <h1>Des prestations propres, claires et faciles a reserver</h1>
          <p>
            Jaankha accompagne les employeurs, les femmes de menage et les espaces
            professionnels avec des offres simples: missions journalieres, suivi mensuel,
            nettoyage specialise et services d entretien.
          </p>
          <div>
            <Link className="button primary" to="/providers">
              Reserver un prestataire
            </Link>
            <Link className="button secondary" to="/register">
              Creer un compte
            </Link>
          </div>
        </div>
        <div className="services-hero-media">
          <img src={servicesHero} alt="Equipe de nettoyage Jaankha" />
        </div>
      </section>

      <section className="services-offers">
        <div className="services-section-heading">
          <p className="eyebrow">Offres</p>
          <h2>Une offre pour chaque besoin</h2>
          <p>Du domicile aux espaces professionnels, Jaankha organise la demande et le suivi.</p>
        </div>
        <div className="services-offers-grid">
          {services.map((service) => (
            <article className="services-offer-card" key={service.title}>
              <span>{service.code}</span>
              <h3>{service.title}</h3>
              <p>{service.body}</p>
              {service.external ? (
                <a href={service.href} rel="noreferrer" target="_blank">
                  {service.actionLabel}
                </a>
              ) : (
                <Link to={service.href}>{service.actionLabel}</Link>
              )}
            </article>
          ))}
        </div>
      </section>

      <section className="services-values">
        <div className="services-section-heading">
          <p className="eyebrow">Valeurs</p>
          <h2>Ce qui guide nos services</h2>
        </div>
        <div className="services-values-grid">
          {values.map((value) => (
            <article key={value.title}>
              <h3>{value.title}</h3>
              <p>{value.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="services-mission-vision">
        <div>
          <p className="eyebrow">Mission</p>
          <h2>Structurer les services de menage et d entretien</h2>
          <ul>
            {missions.map((mission) => (
              <li key={mission}>{mission}</li>
            ))}
          </ul>
        </div>
        <div>
          <p className="eyebrow">Vision</p>
          <h2>Devenir la reference locale des services fiables</h2>
          <p>
            Notre vision est de rendre les services de menage, de nettoyage et
            d assainissement plus accessibles, mieux organises et plus respectueux du
            travail des prestataires.
          </p>
        </div>
      </section>
    </div>
  )
}
