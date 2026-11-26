import { createContext } from 'react'
import type { LoginPayload, RegisterPayload, User } from '../../types/api'

export type AuthContextValue = {
  token: string | null
  user: User | null
  isAuthenticated: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<User>
  logout: () => void
  // Remplace le compte connecte (ex. compte valide par le super administrateur).
  refreshUser: (user: User) => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
