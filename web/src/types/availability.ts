import type { DayOfWeek } from './day-of-week'

export type Availability = {
  id?: string
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
}
