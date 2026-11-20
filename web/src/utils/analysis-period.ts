// Periodes d'analyse du dashboard : toujours la periode calendaire en cours
// (mois, trimestre, semestre ou annee contenant la date du jour).
export type AnalysisPeriod = 'month' | 'quarter' | 'semester' | 'year'

export const analysisPeriods: { id: AnalysisPeriod; label: string }[] = [
  { id: 'month', label: 'Mois' },
  { id: 'quarter', label: 'Trimestre' },
  { id: 'semester', label: 'Semestre' },
  { id: 'year', label: 'Année' },
]

export type PeriodRange = {
  // Bornes en heure locale : start inclus, end exclu.
  start: Date
  end: Date
  // Libelle affiche sous les indicateurs (ex. "Novembre 2026", "T4 2026", "Année 2026").
  label: string
}

const monthFormatter = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' })

export function periodRange(period: AnalysisPeriod, now: Date = new Date()): PeriodRange {
  const year = now.getFullYear()
  const month = now.getMonth()

  switch (period) {
    case 'month': {
      const start = new Date(year, month, 1)
      const label = monthFormatter.format(start)
      return { start, end: new Date(year, month + 1, 1), label: label.charAt(0).toUpperCase() + label.slice(1) }
    }
    case 'quarter': {
      const quarter = Math.floor(month / 3)
      return { start: new Date(year, quarter * 3, 1), end: new Date(year, quarter * 3 + 3, 1), label: `T${quarter + 1} ${year}` }
    }
    case 'semester': {
      const semester = month < 6 ? 0 : 1
      return { start: new Date(year, semester * 6, 1), end: new Date(year, semester * 6 + 6, 1), label: `S${semester + 1} ${year}` }
    }
    case 'year':
      return { start: new Date(year, 0, 1), end: new Date(year + 1, 0, 1), label: `Année ${year}` }
  }
}

// Vrai si la date (ISO-8601) tombe dans la periode. Sans periode, tout est retenu ;
// avec une periode, un element sans date est exclu (on ne peut pas le situer).
export function isWithinPeriod(isoDate: string | null | undefined, range: PeriodRange | null) {
  if (!range) {
    return true
  }
  if (!isoDate) {
    return false
  }
  const date = new Date(isoDate)
  return !Number.isNaN(date.getTime()) && date >= range.start && date < range.end
}
