export type Role = 'EMPLOYEUR' | 'PRESTATAIRE' | 'ADMIN' | 'SUPER_ADMIN'

// Roles d'un compte cree par inscription ou par un administrateur.
export type UserRole = 'EMPLOYEUR' | 'PRESTATAIRE'

// Roles qu'un compte d'administration peut attribuer (ADMIN reserve au super administrateur).
export type CreatableRole = UserRole | 'ADMIN'
