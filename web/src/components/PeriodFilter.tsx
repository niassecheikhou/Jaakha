import { analysisPeriods, type AnalysisPeriod } from '../utils/analysis-period'

type PeriodFilterProps = {
  // null : aucune periode, toutes les donnees sont prises en compte.
  value: AnalysisPeriod | null
  onChange: (period: AnalysisPeriod | null) => void
}

// Barre "Periode d'analyse" : Mois / Trimestre / Semestre / Annee + reinitialisation.
export function PeriodFilter({ value, onChange }: PeriodFilterProps) {
  return (
    <div className="period-filter" role="group" aria-label="Période d'analyse">
      <span className="period-filter-label">Période d'analyse</span>
      <div className="period-filter-options">
        {analysisPeriods.map((period) => (
          <button
            aria-pressed={value === period.id}
            className={value === period.id ? 'active' : ''}
            key={period.id}
            type="button"
            onClick={() => onChange(value === period.id ? null : period.id)}
          >
            {period.label}
          </button>
        ))}
      </div>
      <button className="period-filter-reset" disabled={value === null} type="button" onClick={() => onChange(null)}>
        Réinitialiser les filtres
      </button>
    </div>
  )
}
