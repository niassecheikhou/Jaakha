import type { ProviderProfile, User } from '../../types/api'

export function isProfileComplete(user: User) {
  const hasCommonProfile = Boolean(
    user.photoUrl
      && user.identityPhotoUrl
      && user.identityPhotoBackUrl
      && user.fullName
      && user.phone
      && user.zone,
  )

  if (user.role === 'EMPLOYEUR') {
    return hasCommonProfile
  }

  return Boolean(
    hasCommonProfile
      && user.bio
      && user.monthlyRate
      && user.monthlyRate > 0
      && user.skills.length > 0
      && user.guardianFullName
      && user.guardianPhone,
  )
}

function isAccountPendingValidation(user: User) {
  return (user.role === 'EMPLOYEUR' || user.role === 'PRESTATAIRE') && !user.active && !user.archived
}

export function countPendingProfileApprovals(users: User[], providers: ProviderProfile[]) {
  const pendingUserIds = new Set(users.filter(isAccountPendingValidation).map((user) => user.id))
  const pendingProviderProfileCount = providers.filter((provider) => (
    provider.status === 'PENDING' && !pendingUserIds.has(provider.userId)
  )).length

  return pendingUserIds.size + pendingProviderProfileCount
}

export function displayRole(role: User['role']) {
  if (role === 'EMPLOYEUR') {
    return 'Employeur'
  }
  if (role === 'PRESTATAIRE') {
    return 'Prestataire'
  }
  if (role === 'SUPER_ADMIN') {
    return 'Super administrateur'
  }
  return 'Administrateur'
}

export function initials(fullName: string) {
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
}
