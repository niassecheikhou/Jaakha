import { MailIcon, PhoneIcon, PinIcon } from './ContactIcons'

export const contactCards = [
  {
    icon: <PhoneIcon />,
    label: 'Contactez-nous',
    value: '+221 765057312',
    href: 'tel:+221765057312',
  },
  {
    icon: <MailIcon />,
    label: 'Ecrivez-nous',
    value: 'niassecheikhou@gmail.com',
    href: 'mailto:niassecheikhou@gmail.com',
  },
  {
    icon: <PinIcon />,
    label: 'Notre emplacement',
    value: 'Parcelles Assainies, Dakar, Senegal',
    href: 'https://www.google.com/maps/search/?api=1&query=Dakar%2C%20Senegal',
  },
]
