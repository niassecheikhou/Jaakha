import type { UserRole } from './role'

export type RegisterPayload = {
  fullName: string
  email: string
  phone: string
  password: string
  role: UserRole
}
