import { Link } from 'react-router-dom'
import heroImg from '../assets/hero.png'

const aboutStats = [
  { value: '500+', label: 'Prestataires vises' },
  { value: '14', label: 'Zones de Dakar' },
  { value: '1', label: 'Ville couverte' },
  { value: '4,8/5', label: 'Objectif qualite' },
]

const missions = [
  {
    title: 'Acces simple',
    body: 'Aider les familles dakaroises a trouver rapidement des prestataires fiables.',
  },
  {
    title: 'Suivi clair',
    body: "Permettre a chaque demande de reservation d'avoir un statut lisible.",
  },
  {
    title: 'Profils verifies',
    body: 'Donner plus de confiance grace a la validation des comptes et des dossiers.',
  },
]

const values = [
  {
    icon: 'CO',
    title: 'Confiance',
    body: 'Des profils controles, des avis rattaches aux missions et des statuts transparents.',
  },
  {
    icon: 'DK',
    title: 'Proximite',
    body: 'Une plateforme pensee pour les besoins concrets des quartiers de Dakar.',
  },
  {
    icon: 'CL',
    title: 'Clarite',
    body: 'Des informations simples pour choisir, reserver et suivre chaque prestation.',
  },
  {
    icon: 'DI',
    title: 'Dignite',
    body: 'Valoriser le travail local et donner plus de visibilite aux prestataires.',
  },
]

const milestones = [
  {
    year: '2024',
    title: 'Naissance de l idee',
    body: 'Jaankha part d un besoin local: rendre les services a domicile plus fiables a Dakar.',
  },
  {
    year: '2025',
    title: 'Construction du reseau',
    body: 'Structuration des profils prestataires, des reservations et du processus de validation.',
  },
  {
    year: '2026',
    title: 'Plateforme a Dakar',
    body: 'Lancement d une experience web pour connecter employeurs et prestataires dans Dakar.',
  },
]

export function AboutPage() {
  return (
    <div className="about-page">
      <section className="about-hero">
        <h1>
          Des services fiables
          <span> pour Dakar</span>
        </h1>
        <p>
          Jaankha est une plateforme locale qui connecte les familles, employeurs et
          prestataires de services a domicile. Notre mission est de rendre la recherche,
          la reservation et le suivi plus simples, plus transparents et plus humains.
        </p>
      </section>

      <section className="about-stats" aria-label="Chiffres cles Jaankha">
        {aboutStats.map((item) => (
          <div key={item.label}>
            <strong>{item.value}</strong>
            <span>{item.label}</span>
          </div>
        ))}
      </section>

      <section className="about-mission">
        <div className="about-copy">
          <span className="about-pill">Nos missions</span>
          <h2>Organiser les services a domicile a Dakar</h2>
          <p>
            Nous croyons que chaque famille merite de trouver une aide fiable sans
            perdre du temps, et que chaque prestataire serieux merite une meilleure
            visibilite. Jaankha rassemble les profils, les demandes et les avis dans un
            parcours simple.
          </p>
          <div className="about-mission-list">
            {missions.map((mission) => (
              <article key={mission.title}>
                <span>✓</span>
                <div>
                  <h3>{mission.title}</h3>
                  <p>{mission.body}</p>
                </div>
              </article>
            ))}
          </div>
        </div>
        <div className="about-visual">
          <img src={heroImg} alt="Illustration Jaankha des services a domicile a Dakar" />
          <div className="about-floating-card">
            <span>DKR</span>
            <strong>Dakar uniquement</strong>
            <p>Plateforme locale</p>
          </div>
        </div>
      </section>

      <section className="about-values">
        <div className="about-section-heading">
          <h2>Nos valeurs</h2>
          <p>Les principes qui guident chaque reservation sur Jaankha.</p>
        </div>
        <div className="about-values-grid">
          {values.map((value) => (
            <article key={value.title}>
              <span>{value.icon}</span>
              <h3>{value.title}</h3>
              <p>{value.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="about-journey">
        <div className="about-section-heading">
          <h2>Notre parcours</h2>
          <p>L evolution de Jaankha, ancree a Dakar.</p>
        </div>
        <div className="about-timeline">
          {milestones.map((item, index) => (
            <article className={index % 2 === 0 ? 'left' : 'right'} key={item.year}>
              <span className="timeline-dot" />
              <div>
                <strong>{item.year}</strong>
                <h3>{item.title}</h3>
                <p>{item.body}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="about-cta">
        <h2>Pret a trouver un prestataire de confiance ?</h2>
        <p>Explorez les profils disponibles a Dakar et envoyez votre demande en quelques clics.</p>
        <div>
          <Link className="button secondary" to="/providers">
            Trouver un prestataire
          </Link>
          <Link className="button secondary" to="/register">
            Creer mon compte
          </Link>
        </div>
      </section>
    </div>
  )
}
