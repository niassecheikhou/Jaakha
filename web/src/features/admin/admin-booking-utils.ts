export function bookingReference(id: string) {
  return `#BK-${id.replace(/-/g, '').slice(0, 5).toUpperCase()}`
}
