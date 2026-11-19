import type { User } from '../types/api'

const TOKEN_KEY = 'jaankha.auth.token'
const USER_KEY = 'jaankha.auth.user'

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser() {
  const rawUser = localStorage.getItem(USER_KEY)
  if (!rawUser) {
    return null
  }
  try {
    return JSON.parse(rawUser) as User
  } catch {
    clearStoredAuth()
    return null
  }
}

export function storeAuth(token: string, user: User) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearStoredAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
