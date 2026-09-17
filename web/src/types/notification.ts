export type Notification = {
  id: string
  type: string
  title: string
  message: string
  bookingId: string | null
  userId: string | null
  read: boolean
  createdAt: string
}
