import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import slideOneImg from '../assets/slide1.jpg'
import slideTwoImg from '../assets/slide2.jpg'
import slideThreeImg from '../assets/slide3.jpg'

const heroSlides = [
  {
    eyebrow: 'Services de confiance a Dakar',
    title: 'Tout ce dont vous avez besoin',
    highlight: ' pour trouver le bon prestataire',
    body: 'Jaankha connecte les familles et les professionnels avec des prestataires locaux pour des missions claires, suivies et evaluees.',
    ticketTitle: 'Reservation suivie',
    ticketSteps: 'DEMANDE -> ACCEPTE -> TERMINE',
    badgeValue: '4.8/5',
    badgeLabel: 'avis',
    image: slideOneImg,
    pills: ['Profils verifies', 'Avis fiables'],
    tone: 'aqua',
  },
  {
    eyebrow: 'Reservation simple et rapide',
    title: 'Passez de la recherche',
    highlight: ' a une mission bien organisee',
    body: 'Comparez les profils, choisissez la bonne disponibilite et envoyez une demande sans multiplier les appels.',
    ticketTitle: 'Parcours clair',
    ticketSteps: 'BESOIN -> PROFIL -> RESERVATION',
    badgeValue: '14',
    badgeLabel: 'zones',
    image: slideTwoImg,
    pills: ['Disponibilites', 'Tarifs visibles'],
    tone: 'mint',
  },
  {
    eyebrow: 'Opportunites pour les prestataires',
    title: 'Gagnez en visibilite',
    highlight: ' aupres des familles locales',
    body: 'Les professionnels presentent leurs services, recoivent des demandes et construisent leur reputation mission apres mission.',
    ticketTitle: 'Confiance locale',
    ticketSteps: 'SERVICE -> AVIS -> RECOMMANDATION',
    badgeValue: '500+',
    badgeLabel: 'profils',
    image: slideThreeImg,
    pills: ['Missions suivies', 'Reputation'],
    tone: 'navy',
  },
]

const serviceStats = [
  { value: '500+', label: 'Prestataires inscrits' },
  { value: '14', label: 'Zones couvertes a Dakar' },
  { value: '1K+', label: 'Reservations visees' },
  { value: '4.8/5', label: 'Objectif satisfaction' },
]

const services = [
  {
    title: 'Profils verifies',
    body: 'Chaque prestataire peut etre controle et valide avant d’apparaitre dans la recherche publique.',
    icon: 'ID',
  },
  {
    title: 'Reservation claire',
    body: 'Les demandes suivent un statut simple pour savoir exactement ou en est la prestation.',
    icon: 'RDV',
  },
  {
    title: 'Recherche locale',
    body: 'Filtrez par zone, tarif, disponibilite et competence pour trouver la bonne personne rapidement.',
    icon: 'DKR',
  },
  {
    title: 'Avis apres mission',
    body: 'Les notes sont rattachees aux prestations terminees pour garder des retours utiles et fiables.',
    icon: '5/5',
  },
]

const steps = [
  'Cherchez un prestataire selon votre zone et votre besoin.',
  'Consultez son profil, son tarif, ses disponibilites et ses avis.',
  'Envoyez une demande de reservation et suivez son statut.',
  'Notez la prestation une fois la mission terminee.',
]

const partners = ['Dakar Services', 'Teranga RH', 'Maison Pro', 'Djom App', 'Sunu Work', 'Baobab Care']

const testimonials = [
  {
    quote: 'J’ai pu trouver une aide fiable pour la maison sans passer par dix appels.',
    name: 'Aissatou',
    role: 'Employeure a Mermoz',
  },
  {
    quote: 'Le suivi des demandes rend les missions beaucoup plus simples a organiser.',
    name: 'Mamadou',
    role: 'Prestataire a Grand Yoff',
  },
  {
    quote: 'Les profils avec avis aident a choisir avec plus de confiance.',
    name: 'Fatou',
    role: 'Employeure a Plateau',
  },
]

export function HomePage() {
  const [activeSlide, setActiveSlide] = useState(0)

  useEffect(() => {
    const timer = window.setInterval(() => {
      setActiveSlide((currentSlide) => (currentSlide + 1) % heroSlides.length)
    }, 5200)

    return () => window.clearInterval(timer)
  }, [])

  const showPreviousSlide = () => {
    setActiveSlide((currentSlide) =>
      currentSlide === 0 ? heroSlides.length - 1 : currentSlide - 1,
    )
  }

  const showNextSlide = () => {
    setActiveSlide((currentSlide) => (currentSlide + 1) % heroSlides.length)
  }

  return (
    <div className="public-home">
      <section
        className="public-hero"
        aria-label="Banniere Jaankha"
        aria-roledescription="carousel"
      >
        <div className="hero-slides">
          {heroSlides.map((slide, index) => {
            const isActive = activeSlide === index

            return (
              <article
                className={`hero-slide hero-slide-${slide.tone} ${isActive ? 'active' : ''}`}
                key={slide.title}
                aria-hidden={!isActive}
              >
                <img className="hero-slide-image" src={slide.image} alt="" aria-hidden="true" />
                <div className="hero-copy">
                  <p className="eyebrow">{slide.eyebrow}</p>
                  <h1>
                    {slide.title}
                    <span>{slide.highlight}</span>
                  </h1>
                  <p>{slide.body}</p>
                  <div className="hero-actions">
                    <Link className="button primary" tabIndex={isActive ? 0 : -1} to="/providers">
                      Trouver un prestataire
                    </Link>
                    <Link className="button secondary" tabIndex={isActive ? 0 : -1} to="/register">
                      Devenir prestataire
                    </Link>
                  </div>
                  <div className="hero-proof" aria-hidden="true">
                    <div className="hero-proof-avatars">
                      <span>AD</span>
                      <span>MK</span>
                      <span>FS</span>
                    </div>
                    <strong>Clients satisfaits</strong>
                    <span>Service local suivi et evalue</span>
                  </div>
                </div>
                <div className={`hero-media hero-media-${slide.tone}`} aria-hidden="true">
                  <div className="hero-badge">
                    <strong>{slide.badgeValue}</strong>
                    <span>{slide.badgeLabel}</span>
                  </div>
                  <div className="hero-pills">
                    {slide.pills.map((pill) => (
                      <span key={pill}>{pill}</span>
                    ))}
                  </div>
                  <div className="hero-ticket">
                    <strong>{slide.ticketTitle}</strong>
                    <span>{slide.ticketSteps}</span>
                  </div>
                </div>
              </article>
            )
          })}
        </div>
        <div className="hero-slider-controls" aria-label="Controle de la banniere">
          <button type="button" onClick={showPreviousSlide} aria-label="Slide precedente">
            <span aria-hidden="true">&lsaquo;</span>
          </button>
          <div className="hero-slider-dots">
            {heroSlides.map((slide, index) => (
              <button
                className={activeSlide === index ? 'active' : ''}
                type="button"
                key={slide.title}
                onClick={() => setActiveSlide(index)}
                aria-label={`Afficher la slide ${index + 1}`}
                aria-current={activeSlide === index}
              />
            ))}
          </div>
          <button type="button" onClick={showNextSlide} aria-label="Slide suivante">
            <span aria-hidden="true">&rsaquo;</span>
          </button>
        </div>
      </section>

      <section className="stats-band" aria-label="Chiffres cles">
        {serviceStats.map((item) => (
          <div key={item.label}>
            <strong>{item.value}</strong>
            <span>{item.label}</span>
          </div>
        ))}
      </section>

      <section className="landing-section">
        <div className="center-heading">
          <p className="eyebrow">Nos services</p>
          <h2>
            Une plateforme complete
            <span> pour reserver sereinement</span>
          </h2>
          <p>
            Une experience simple qui combine recherche, verification, reservation et avis
            pour faciliter les prestations du quotidien.
          </p>
        </div>
        <div className="services-grid">
          {services.map((service) => (
            <article className="service-card" key={service.title}>
              <span className="service-icon">{service.icon}</span>
              <h3>{service.title}</h3>
              <p>{service.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="vision-section">
        <div>
          <p className="eyebrow">Vision</p>
          <h2>Structurer le marche local des services a domicile</h2>
        </div>
        <p>
          Notre ambition est de rendre les prestations locales plus accessibles, plus
          transparentes et plus dignes pour chaque acteur : l’employeur gagne du temps,
          le prestataire gagne en visibilite, et l’administrateur garde un cadre clair
          pour valider, moderer et accompagner.
        </p>
      </section>

      <section className="landing-section">
        <div className="center-heading">
          <p className="eyebrow">Comment ca marche</p>
          <h2>Un parcours en quatre etapes</h2>
        </div>
        <div className="steps-grid">
          {steps.map((step, index) => (
            <article className="step-card" key={step}>
              <span>{index + 1}</span>
              <p>{step}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-section">
        <div className="center-heading">
          <p className="eyebrow">Nos partenaires</p>
          <h2>Un reseau local qui grandit</h2>
        </div>
        <div className="logo-slider" aria-label="Logos partenaires">
          <div className="slider-track">
            {[...partners, ...partners].map((partner, index) => (
              <span className="partner-logo" key={`${partner}-${index}`}>
                {partner}
              </span>
            ))}
          </div>
        </div>
      </section>

      <section className="landing-section">
        <div className="center-heading">
          <p className="eyebrow">Temoignages</p>
          <h2>Ce que les premiers utilisateurs recherchent</h2>
        </div>
        <div className="testimonial-slider">
          <div className="testimonial-track">
            {[...testimonials, ...testimonials].map((testimonial, index) => (
              <article className="testimonial-card" key={`${testimonial.name}-${index}`}>
                <p>{testimonial.quote}</p>
                <strong>{testimonial.name}</strong>
                <span>{testimonial.role}</span>
              </article>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}
