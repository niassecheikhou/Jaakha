export type ApiError = {
  status: number
  message: string
  fields?: Record<string, string>
}
