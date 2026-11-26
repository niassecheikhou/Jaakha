import type { CreatableRole } from './role'

export type AdminCreateUserPayload = {
  fullName: string
  email: string
  phone: string
  password: string
  role: CreatableRole
}
