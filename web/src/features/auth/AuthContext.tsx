import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../../services/api'
import {
  clearStoredAuth,
  getStoredToken,
  getStoredUser,
  storeAuth,
} from '../../utils/auth-storage'
import type { LoginPayload, RegisterPayload, User } from '../../types/api'
import { isAdministrationRole } from '../../utils/roles'
import { AuthContext } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState(() => getStoredToken())
  const [user, setUser] = useState<User | null>(() => getStoredUser())
  const navigate = useNavigate()

  // Au chargement, relit le compte connecte : le role stocke dans le navigateur peut etre perime
  // (ex. compte promu super administrateur). Une session expiree (401) deconnecte l'utilisateur.
  useEffect(() => {
    if (!getStoredToken()) {
      return
    }
    let cancelled = false
    authApi.me()
      .then((freshUser) => {
        if (cancelled) {
          return
        }
        const storedToken = getStoredToken()
        if (storedToken) {
          storeAuth(storedToken, freshUser)
          setUser(freshUser)
        } else {
          setToken(null)
          setUser(null)
        }
      })
      .catch(() => {
        if (!cancelled && !getStoredToken()) {
          setToken(null)
          setUser(null)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (payload: LoginPayload) => {
    const response = await authApi.login(payload)
    storeAuth(response.token, response.user)
    setToken(response.token)
    setUser(response.user)
    navigate(defaultRouteFor(response.user.role), { replace: true })
  }, [navigate])

  const register = useCallback(async (payload: RegisterPayload) => {
    const response = await authApi.register(payload)
    storeAuth(response.token, response.user)
    setToken(response.token)
    setUser(response.user)
    navigate(defaultRouteFor(response.user.role), { replace: true })
    return response.user
  }, [navigate])

  const refreshUser = useCallback((freshUser: User) => {
    const storedToken = getStoredToken()
    if (storedToken) {
      storeAuth(storedToken, freshUser)
      setUser(freshUser)
    }
  }, [])

  const logout = useCallback(() => {
    clearStoredAuth()
    setToken(null)
    setUser(null)
    navigate('/login', { replace: true })
  }, [navigate])

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token && user),
      login,
      register,
      logout,
      refreshUser,
    }),
    [login, logout, refreshUser, register, token, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

function defaultRouteFor(role: User['role']) {
  if (isAdministrationRole(role)) {
    return '/admin'
  }
  if (role === 'PRESTATAIRE') {
    return '/provider'
  }
  return '/employer'
}
