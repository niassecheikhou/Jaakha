import { useState, type FormEvent } from 'react'
import { useToast } from '../components/toast-context'
import { contactCards } from '../features/contact/contact-cards'
import { contactApi, getApiError } from '../services/api'

export function ContactPage() {
  const toast = useToast()
  const [sent, setSent] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const formData = new FormData(form)
    setSent(false)
    setIsSubmitting(true)

    try {
      await contactApi.create({
        name: String(formData.get('name') ?? ''),
        email: String(formData.get('email') ?? ''),
        phone: String(formData.get('phone') ?? ''),
        company: String(formData.get('company') ?? ''),
        message: String(formData.get('message') ?? ''),
      })
      form.reset()
      setSent(true)
      toast.success('Message recu', 'Merci, notre equipe vous recontactera rapidement.')
    } catch (caughtError) {
      toast.error('Message non envoye', getApiError(caughtError).message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="contact-page">
      <section className="contact-hero">
        <p className="eyebrow">Contact</p>
        <h1>Parlons de vos besoins de service</h1>
        <p>
          Une question, une demande de devis ou un besoin regulier ? Notre equipe vous
          accompagne pour trouver la bonne formule.
        </p>
      </section>

      <section className="contact-cards" aria-label="Coordonnees Jaankha">
        {contactCards.map((card) => (
          <article key={card.label}>
            <span>{card.icon}</span>
            <p>{card.label}</p>
            <a href={card.href} rel="noreferrer" target={card.href.startsWith('http') ? '_blank' : undefined}>
              {card.value}
            </a>
          </article>
        ))}
      </section>

      <section className="contact-panel">
        <div className="contact-map" aria-label="Carte de Dakar">
          <iframe
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
            src="https://www.google.com/maps?q=Dakar%2C%20Senegal&output=embed"
            title="Localisation Jaankha a Dakar"
          />
        </div>
        <form className="contact-form" onSubmit={handleSubmit}>
          <div>
            <p className="eyebrow">Devis gratuit</p>
            <h2>Envoyez-nous un message</h2>
          </div>
          <div className="contact-form-grid">
            <input name="name" placeholder="Votre nom" required />
            <input name="email" placeholder="Votre courriel" required type="email" />
            <input name="phone" placeholder="Mobile" />
            <input name="company" placeholder="Entreprise" />
          </div>
          <textarea name="message" placeholder="Message" required rows={7} />
          <button className="button primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Envoi en cours...' : 'Envoyer un message ->'}
          </button>
          {sent && <p className="contact-form-success">Votre message a bien ete pris en compte.</p>}
        </form>
      </section>
    </div>
  )
}
