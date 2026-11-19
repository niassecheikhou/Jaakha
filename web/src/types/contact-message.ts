export type ContactMessage = {
  id: string
  name: string
  email: string
  phone?: string | null
  company?: string | null
  message: string
  createdAt: string
}
