import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { adminApi, getApiError } from '../../services/api'
import type { AuditAction, AuditLogFilters, Role, User } from '../../types/api'
import { displayRole } from './admin-utils'

// Libelles des actions enregistrees par le backend (AuditLogService).
const auditActionLabels: Record<AuditAction, string> = {
  CONNEXION: 'Connexion',
  INSCRIPTION: 'Inscription',
  COMPTE_CREE: 'Compte cree',
  COMPTE_VALIDE: 'Compte valide',
  COMPTE_INVALIDE: 'Compte invalide',
  COMPTE_ARCHIVE: 'Compte archive',
  COMPTE_RESTAURE: 'Compte restaure',
  PROFIL_MIS_A_JOUR: 'Profil mis a jour',
  PHOTO_PROFIL_MODIFIEE: 'Photo de profil modifiee',
  PIECE_IDENTITE_DEPOSEE: "Piece d'identite deposee",
  PROFIL_PRESTATAIRE_MIS_A_JOUR: 'Profil prestataire mis a jour',
  PROFIL_PRESTATAIRE_STATUT: 'Statut du profil prestataire',
  RESERVATION_CREEE: 'Reservation creee',
  RESERVATION_STATUT: 'Statut de reservation',
  AVIS_PUBLIE: 'Avis publie',
}

const roles: Role[] = ['SUPER_ADMIN', 'ADMIN', 'EMPLOYEUR', 'PRESTATAIRE']

type HistoryPeriod = 'day' | 'week' | 'month' | 'quarter' | 'semester' | 'year'

const historyPeriods: { id: HistoryPeriod; label: string }[] = [
  { id: 'day', label: 'Jour' },
  { id: 'week', label: 'Semaine' },
  { id: 'month', label: 'Mois' },
  { id: 'quarter', label: 'Trimestre' },
  { id: 'semester', label: 'Semestre' },
  { id: 'year', label: 'Année' },
]

function todayIso() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

// Periode calendaire (heure locale) contenant la date de reference : start inclus, end exclu.
function historyRange(period: HistoryPeriod, referenceDate: string) {
  const [year, month, day] = referenceDate.split('-').map(Number)
  switch (period) {
    case 'day':
      return { start: new Date(year, month - 1, day), end: new Date(year, month - 1, day + 1) }
    case 'week': {
      // Semaine du lundi au dimanche.
      const reference = new Date(year, month - 1, day)
      const offset = (reference.getDay() + 6) % 7
      return { start: new Date(year, month - 1, day - offset), end: new Date(year, month - 1, day - offset + 7) }
    }
    case 'month':
      return { start: new Date(year, month - 1, 1), end: new Date(year, month, 1) }
    case 'quarter': {
      const first = Math.floor((month - 1) / 3) * 3
      return { start: new Date(year, first, 1), end: new Date(year, first + 3, 1) }
    }
    case 'semester': {
      const first = month <= 6 ? 0 : 6
      return { start: new Date(year, first, 1), end: new Date(year, first + 6, 1) }
    }
    case 'year':
      return { start: new Date(year, 0, 1), end: new Date(year + 1, 0, 1) }
  }
}

const dateTimeFormatter = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'medium' })
const dayFormatter = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' })

function periodCaption(period: HistoryPeriod, start: Date, end: Date) {
  if (period === 'day') {
    return dayFormatter.format(start)
  }
  const lastDay = new Date(end.getFullYear(), end.getMonth(), end.getDate() - 1)
  return `du ${dayFormatter.format(start)} au ${dayFormatter.format(lastDay)}`
}

// Historique des actions des utilisateurs (reserve au super administrateur).
// Rafraichi automatiquement : les nouvelles actions apparaissent sans recharger la page.
export function AdminAuditLog({ users }: { users: User[] }) {
  const [period, setPeriod] = useState<HistoryPeriod | null>(null)
  const [referenceDate, setReferenceDate] = useState(todayIso)
  const [actorId, setActorId] = useState('')
  const [actorRole, setActorRole] = useState<Role | ''>('')
  const [action, setAction] = useState<AuditAction | ''>('')

  const range = period && referenceDate ? historyRange(period, referenceDate) : null
  // TanStack Query compare les cles par valeur : un nouvel objet a chaque rendu ne relance pas la requete.
  const filters: AuditLogFilters = {
    from: range?.start.toISOString(),
    to: range?.end.toISOString(),
    actorId: actorId || undefined,
    actorRole: actorRole || undefined,
    action: action || undefined,
  }

  const logsQuery = useQuery({
    queryKey: ['admin', 'audit-logs', filters],
    queryFn: () => adminApi.auditLogs(filters),
    refetchInterval: 10_000,
  })
  const logs = logsQuery.data ?? []
  const hasFilters = period !== null || actorId !== '' || actorRole !== '' || action !== ''
  const sortedUsers = [...users].sort((first, second) => first.fullName.localeCompare(second.fullName, 'fr'))

  function resetFilters() {
    setPeriod(null)
    setReferenceDate(todayIso())
    setActorId('')
    setActorRole('')
    setAction('')
  }

  return (
    <section className="panel span-2">
      <h2>Historique des actions</h2>
      <p className="muted">Qui a fait quoi, sur quoi et quand. La liste se met a jour automatiquement.</p>

      <div className="period-filter" role="group" aria-label="Période de l'historique">
        <span className="period-filter-label">Période</span>
        <div className="period-filter-options">
          {historyPeriods.map((item) => (
            <button
              aria-pressed={period === item.id}
              className={period === item.id ? 'active' : ''}
              key={item.id}
              type="button"
              onClick={() => setPeriod(period === item.id ? null : item.id)}
            >
              {item.label}
            </button>
          ))}
        </div>
        <label className="audit-reference-date">
          Date de référence
          <input type="date" value={referenceDate} max={todayIso()} onChange={(event) => setReferenceDate(event.target.value || todayIso())} />
        </label>
      </div>

      <div className="audit-filters">
        <label>
          Utilisateur
          <select value={actorId} onChange={(event) => setActorId(event.target.value)}>
            <option value="">Tous les utilisateurs</option>
            {sortedUsers.map((account) => (
              <option key={account.id} value={account.id}>{account.fullName} ({displayRole(account.role)})</option>
            ))}
          </select>
        </label>
        <label>
          Rôle
          <select value={actorRole} onChange={(event) => setActorRole(event.target.value as Role | '')}>
            <option value="">Tous les rôles</option>
            {roles.map((role) => <option key={role} value={role}>{displayRole(role)}</option>)}
          </select>
        </label>
        <label>
          Action
          <select value={action} onChange={(event) => setAction(event.target.value as AuditAction | '')}>
            <option value="">Toutes les actions</option>
            {(Object.keys(auditActionLabels) as AuditAction[]).map((code) => (
              <option key={code} value={code}>{auditActionLabels[code]}</option>
            ))}
          </select>
        </label>
        <button className="period-filter-reset" disabled={!hasFilters} type="button" onClick={resetFilters}>
          Réinitialiser les filtres
        </button>
      </div>

      <p className="audit-summary">
        <strong>{logs.length.toLocaleString('fr-FR')}</strong> action(s)
        {period && range ? ` ${periodCaption(period, range.start, range.end)}` : ' depuis le début'}
        {logs.length >= 1000 && ' (1 000 plus recentes affichees : affine les filtres)'}
      </p>

      {logsQuery.isError && <p className="empty-state">{getApiError(logsQuery.error).message}</p>}
      {logsQuery.isLoading && <p className="muted">Chargement de l'historique...</p>}
      {!logsQuery.isLoading && !logsQuery.isError && logs.length === 0 && (
        <p className="empty-state">Aucune action ne correspond a ces filtres.</p>
      )}
      {logs.length > 0 && (
        <div className="admin-table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Utilisateur</th>
                <th>Action</th>
                <th>Cible</th>
                <th>Détails</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td className="audit-date">{dateTimeFormatter.format(new Date(log.createdAt))}</td>
                  <td>
                    <strong>{log.actorName || 'Inconnu'}</strong>
                    {log.actorRole && (
                      <span className={`role-pill role-${log.actorRole.toLowerCase()} audit-role`}>{displayRole(log.actorRole)}</span>
                    )}
                  </td>
                  <td>{auditActionLabels[log.action] ?? log.action}</td>
                  <td>{log.targetLabel || '-'}</td>
                  <td>{log.details || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
