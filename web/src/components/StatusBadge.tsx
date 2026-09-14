import { formatStatus } from '../utils/format'
import type { BookingStatus, ProviderStatus } from '../types/api'

export function StatusBadge({ status }: { status: BookingStatus | ProviderStatus }) {
  return <span className={`status status-${status.toLowerCase()}`}>{formatStatus(status)}</span>
}
