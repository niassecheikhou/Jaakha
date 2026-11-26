import type { Role } from './role'

// Une entree de l'historique : qui (auteur) a fait quoi (action), sur quoi (cible), et quand.
export type AuditLog = {
  id: string
  actorId: string | null
  actorName: string | null
  actorRole: Role | null
  action: AuditAction
  targetType: string | null
  targetId: string | null
  targetLabel: string | null
  details: string | null
  // Date de l'action (ISO-8601).
  createdAt: string
}

export type AuditAction =
  | 'CONNEXION'
  | 'INSCRIPTION'
  | 'COMPTE_CREE'
  | 'COMPTE_VALIDE'
  | 'COMPTE_INVALIDE'
  | 'COMPTE_ARCHIVE'
  | 'COMPTE_RESTAURE'
  | 'PROFIL_MIS_A_JOUR'
  | 'PHOTO_PROFIL_MODIFIEE'
  | 'PIECE_IDENTITE_DEPOSEE'
  | 'PROFIL_PRESTATAIRE_MIS_A_JOUR'
  | 'PROFIL_PRESTATAIRE_STATUT'
  | 'RESERVATION_CREEE'
  | 'RESERVATION_STATUT'
  | 'AVIS_PUBLIE'

// Filtres de recherche (tous optionnels). Periode : [from, to[ en ISO-8601.
export type AuditLogFilters = {
  from?: string
  to?: string
  actorId?: string
  actorRole?: Role
  action?: AuditAction
}
