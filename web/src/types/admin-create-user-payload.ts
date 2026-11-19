import type { Role } from './role'

export type AdminCreateUserPayload = {
  fullName: string
  email: string
  phone: string
  password: string
  role: Exclude<Role, 'ADMIN'>
}
