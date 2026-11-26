// Reservation creee par un administrateur pour le compte d'un employeur qu'il gere.
export type AdminBookingPayload = {
  employerId: string
  providerId: string
  // Date (AAAA-MM-JJ) et heure de debut (HH:mm) de la prestation.
  serviceDate: string
  startTime: string
  durationHours: number
  address: string
  instructions: string
  // true : la reservation est acceptee des sa creation.
  validate: boolean
}
