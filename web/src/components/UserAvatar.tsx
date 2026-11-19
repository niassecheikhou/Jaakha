import { useQuery } from '@tanstack/react-query'
import { accountApi } from '../services/api'

type UserAvatarProps = {
  name: string
  className?: string
}

// Avatar de l'utilisateur connecte : sa photo de profil, sinon ses initiales.
// Partage le cache ['account', 'me'] : il se met a jour des qu'une nouvelle photo est envoyee.
export function UserAvatar({ name, className = 'user-avatar' }: UserAvatarProps) {
  const profileQuery = useQuery({
    queryKey: ['account', 'me'],
    queryFn: accountApi.mine,
    retry: false,
  })
  const photoUrl = profileQuery.data?.photoUrl

  if (photoUrl) {
    return <img className={className} src={photoUrl} alt={`Photo de profil de ${name}`} />
  }
  return (
    <span className={`${className} user-avatar-initials`} aria-label={`Pas encore de photo de profil pour ${name}`} role="img">
      {initials(name)}
    </span>
  )
}

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) {
    return '?'
  }
  const first = parts[0][0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1][0] ?? '' : ''
  return (first + last).toUpperCase()
}
