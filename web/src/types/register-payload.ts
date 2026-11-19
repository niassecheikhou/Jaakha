import type { Role } from './role'

export type RegisterPayload = {
  fullName: string
  email: string
  phone: string
  password: string
  role: Exclude<Role, 'ADMIN'>
}
