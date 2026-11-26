import type { Role } from '../types/role'

// Compte d'administration : administrateur ou super administrateur.
export function isAdministrationRole(role: Role | undefined | null) {
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
}

export function isSuperAdminRole(role: Role | undefined | null) {
  return role === 'SUPER_ADMIN'
}
