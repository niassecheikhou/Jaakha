import axios, { AxiosError } from 'axios'
import {
  clearStoredAuth,
  getStoredToken,
  storeAuth,
} from '../utils/auth-storage'
import { api } from './costante'
import type {
  AccountProfile,
  AccountProfilePayload,
  AdminCreateUserPayload,
  ApiError,
  AuditLog,
  AuditLogFilters,
  AuthResponse,
  Booking,
  BookingPayload,
  BookingStatus,
  ContactMessage,
  ContactMessagePayload,
  ForgotPasswordPayload,
  IdentityPhotosPayload,
  IdentityPhotosResponse,
  LoginPayload,
  MessageResponse,
  Notification,
  PhotoUploadResponse,
  ProviderProfile,
  ProviderProfilePayload,
  ProviderStatus,
  RegisterPayload,
  ResetPasswordPayload,
  Review,
  ReviewPayload,
  User,
} from '../types/api'

export { api } from './costante'

api.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      clearStoredAuth()
    }
    return Promise.reject(error)
  },
)

export function getApiError(error: unknown) {
  if (axios.isAxiosError<ApiError>(error)) {
    if (!error.response) {
      return {
        message: "Impossible de joindre le backend. Verifie que l'API est demarree sur http://localhost:8080 et que VITE_API_BASE_URL pointe vers /api.",
        fields: {},
      }
    }

    const status = error.response.status
    const responseData = error.response.data as ApiError & {
      detail?: string
      title?: string
    }
    const backendMessage = responseData?.message || responseData?.detail || responseData?.title

    if (backendMessage) {
      return {
        message: backendMessage,
        fields: responseData.fields ?? {},
      }
    }

    const fallbackMessages: Record<number, string> = {
      400: 'La requete est invalide. Verifie les champs envoyes.',
      401: 'Ta session a expire. Connecte-toi puis reessaie.',
      403: "Action refusee par le backend. Cause probable en local: ancien backend encore lance ou CORS bloque parce que le web tourne sur un autre port. Redemarre l'API apres les dernieres modifications.",
      404: "L'endpoint appele est introuvable. Redemarre le backend pour charger les nouvelles routes, puis reessaie.",
      413: 'Le fichier est trop lourd. Choisis une image plus petite.',
      415: 'Le format du fichier est refuse. Utilise une image JPG, PNG ou WEBP.',
      500: 'Erreur serveur pendant le traitement. Consulte les logs du backend.',
    }

    return {
      message: fallbackMessages[status] ?? `Erreur API ${status}. Merci de verifier la requete et les logs du backend.`,
      fields: responseData?.fields ?? {},
    }
  }
  return { message: 'Une erreur inattendue est survenue.', fields: {} }
}

export const authApi = {
  async register(payload: RegisterPayload) {
    const { data } = await api.post<AuthResponse>('/auth/register', payload)
    return data
  },
  async login(payload: LoginPayload) {
    const { data } = await api.post<AuthResponse>('/auth/login', payload)
    storeAuth(data.token, data.user)
    return data
  },
  // Compte connecte relu cote serveur (role et informations a jour).
  async me() {
    const { data } = await api.get<User>('/auth/me')
    return data
  },
  async forgotPassword(payload: ForgotPasswordPayload) {
    const { data } = await api.post<MessageResponse>('/auth/forgot-password', payload)
    return data
  },
  async resetPassword(payload: ResetPasswordPayload) {
    const { data } = await api.post<MessageResponse>('/auth/reset-password', payload)
    return data
  },
}

export const providersApi = {
  async search(params: Record<string, string>) {
    const { data } = await api.get<ProviderProfile[]>('/providers', { params })
    return data
  },
  async detail(id: string) {
    const { data } = await api.get<ProviderProfile>(`/providers/${id}`)
    return data
  },
  async mine() {
    const { data } = await api.get<ProviderProfile>('/providers/me')
    return data
  },
  async saveMine(payload: ProviderProfilePayload) {
    const { data } = await api.put<ProviderProfile>('/providers/me', payload)
    return data
  },
  async reviews(id: string) {
    const { data } = await api.get<Review[]>(`/providers/${id}/reviews`)
    return data
  },
}

export const accountApi = {
  async mine() {
    const { data } = await api.get<AccountProfile>('/account/me')
    return data
  },
  async save(payload: AccountProfilePayload) {
    const { data } = await api.put<AccountProfile>('/account/me', payload)
    return data
  },
  async uploadPhoto(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await api.post<PhotoUploadResponse>('/account/me/photo', formData)
    return data
  },
  // Recto et/ou verso de la piece d'identite en une seule requete.
  async uploadIdentityPhotos(files: IdentityPhotosPayload) {
    const formData = new FormData()
    if (files.front) {
      formData.append('front', files.front)
    }
    if (files.back) {
      formData.append('back', files.back)
    }
    const { data } = await api.post<IdentityPhotosResponse>('/account/me/identity-photos', formData)
    return data
  },
}

export const bookingsApi = {
  async create(payload: BookingPayload) {
    const { data } = await api.post<Booking>('/bookings', payload)
    return data
  },
  async mine() {
    const { data } = await api.get<Booking[]>('/bookings/me')
    return data
  },
  async updateStatus(id: string, status: BookingStatus) {
    const { data } = await api.patch<Booking>(`/bookings/${id}/status`, {
      status,
    })
    return data
  },
}

export const privateFilesApi = {
  // Les chemins renvoyes par le backend commencent par /api : on les resout depuis l'origine de l'API.
  async image(path: string) {
    const apiOrigin = new URL(api.defaults.baseURL ?? '', window.location.origin).origin
    const { data } = await api.get<Blob>(path, { baseURL: apiOrigin, responseType: 'blob' })
    return data
  },
}

export const reviewsApi = {
  async create(payload: ReviewPayload) {
    const { data } = await api.post<Review>('/reviews', payload)
    return data
  },
}

export const contactApi = {
  async create(payload: ContactMessagePayload) {
    const { data } = await api.post<ContactMessage>('/contact/messages', payload)
    return data
  },
}

export const notificationsApi = {
  async list() {
    const { data } = await api.get<Notification[]>('/notifications')
    return data
  },
  async markRead(id: string) {
    const { data } = await api.patch<Notification>(`/notifications/${id}/read`)
    return data
  },
}

export const adminApi = {
  async users() {
    const { data } = await api.get<User[]>('/admin/users')
    return data
  },
  async createUser(payload: AdminCreateUserPayload) {
    const { data } = await api.post<User>('/admin/users', payload)
    return data
  },
  async providers(status?: ProviderStatus) {
    const { data } = await api.get<ProviderProfile[]>('/admin/providers', {
      params: status ? { status } : undefined,
    })
    return data
  },
  async bookings(status?: BookingStatus) {
    const { data } = await api.get<Booking[]>('/admin/bookings', {
      params: status ? { status } : undefined,
    })
    return data
  },
  // Historique des actions (super administrateur), filtres optionnels.
  async auditLogs(filters: AuditLogFilters) {
    const { data } = await api.get<AuditLog[]>('/admin/audit-logs', { params: filters })
    return data
  },
  async contactMessages() {
    const { data } = await api.get<ContactMessage[]>('/admin/contact-messages')
    return data
  },
  async exportContactMessages(format: 'csv' | 'excel') {
    const { data } = await api.get<Blob>('/admin/contact-messages/export', {
      params: { format },
      responseType: 'blob',
    })
    return data
  },
  async updateProviderStatus(id: string, status: ProviderStatus) {
    const { data } = await api.patch<ProviderProfile>(
      `/admin/providers/${id}/status`,
      { status },
    )
    return data
  },
  async updateUserActive(id: string, active: boolean) {
    const { data } = await api.patch<User>(`/admin/users/${id}/active`, {
      active,
    })
    return data
  },
  async updateUserArchived(id: string, archived: boolean) {
    const { data } = await api.patch<User>(`/admin/users/${id}/archive`, {
      archived,
    })
    return data
  },
  async updateBookingStatus(id: string, status: BookingStatus) {
    const { data } = await api.patch<Booking>(`/admin/bookings/${id}/status`, {
      status,
    })
    return data
  },
}
