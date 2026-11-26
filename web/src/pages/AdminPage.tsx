import { AdminDashboard } from '../features/admin/AdminDashboard'
import { AdminProfilePanel } from '../features/admin/AdminProfilePanel'
import { useAuth } from '../hooks/use-auth'

export function AdminPage() {
  const { user } = useAuth()

  // Administrateur pas encore valide : il complete son dossier, l'administration lui reste fermee.
  if (user?.role === 'ADMIN' && !user.active) {
    return (
      <div className="dashboard-grid">
        <section className="panel span-2">
          <div className="notice">
            <strong>Compte administrateur en attente de validation</strong>
            <span>
              Renseigne ta piece d'identite (recto et verso), ton adresse et ta localisation.
              Le super administrateur validera ensuite ton compte et tu auras acces a l'administration
              (recharge la page apres la validation).
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
