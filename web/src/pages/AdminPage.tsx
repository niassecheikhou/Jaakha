import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { AdminDashboard } from '../features/admin/AdminDashboard'
import { AdminProfilePanel } from '../features/admin/AdminProfilePanel'
import { useAuth } from '../hooks/use-auth'
import { authApi } from '../services/api'

export function AdminPage() {
  const { user, refreshUser } = useAuth()
  const isPendingAdmin = user?.role === 'ADMIN' && !user.active

  // Suivi du compte administrateur sans recharger la page : toutes les 5 s tant qu'il attend la
  // validation (il bascule sur le dashboard des que le super admin le valide), puis toutes les 30 s
  // (un admin invalide repasse aussitot sur l'ecran d'attente).
  const meQuery = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: authApi.me,
    enabled: user?.role === 'ADMIN',
    refetchInterval: isPendingAdmin ? 5_000 : 30_000,
  })
  const freshUser = meQuery.data
  useEffect(() => {
    if (freshUser && user && (freshUser.active !== user.active || freshUser.role !== user.role)) {
      refreshUser(freshUser)
    }
  }, [freshUser, refreshUser, user])

  // Administrateur pas encore valide : il complete son dossier, l'administration lui reste fermee.
  if (isPendingAdmin) {
    return (
      <div className="dashboard-grid">
        <section className="panel span-2">
          <div className="notice">
            <strong>Compte administrateur en attente de validation</strong>
            <span>
              Renseigne ta piece d'identite (recto et verso), ton adresse et ta localisation.
              Des que le super administrateur aura valide ton compte, ton tableau de bord s'affichera
              automatiquement sur cette page.
            </span>
          </div>
        </section>
        <div className="span-2">
          <AdminProfilePanel />
        </div>
      </div>
    )
  }

  return <AdminDashboard />
}
