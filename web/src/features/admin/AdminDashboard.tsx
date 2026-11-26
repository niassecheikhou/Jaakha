import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FormError } from '../../components/FormError'
import { PeriodFilter } from '../../components/PeriodFilter'
import { useToast } from '../../components/toast-context'
import { WorkspaceShell, type WorkspaceNavItem } from '../../components/WorkspaceShell'
import { useAuth } from '../../hooks/use-auth'
import { adminApi, getApiError } from '../../services/api'
import type { AdminCreateUserPayload, BookingStatus, ProviderStatus, User } from '../../types/api'
import { isWithinPeriod, periodRange, type AnalysisPeriod } from '../../utils/analysis-period'
import { formatStatus } from '../../utils/format'
import { AccountsTable, UserDetailPanel } from './AdminAccounts'
import {
  HistoryIcon,
  BagIcon,
  DashboardIcon,
  HourglassIcon,
  MailIcon,
  ProfilePendingIcon,
  UsersIcon,
} from './AdminIcons'
import { BookingMetricCard, BookingsManagement, DashboardOverview } from './AdminBookings'
import { AdminContactMessages } from './AdminContactMessages'
import { AdminProfilePanel } from './AdminProfilePanel'
import { ProfileIcon } from '../employer/EmployerIcons'
import { ProviderProfilesReview } from './AdminProviders'
import { AdminAuditLog } from './AdminAuditLog'
import { CreateUserForm } from './CreateUserForm'
import { CsvUsersImport } from './CsvUsersImport'
import { bookingReference } from './admin-booking-utils'
import type { AdminMenu } from './admin-types'
import { countPendingProfileApprovals } from './admin-utils'
import { isAdministrationRole, isSuperAdminRole } from '../../utils/roles'

export function AdminDashboard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const toast = useToast()
  const [activeMenu, setActiveMenu] = useState<AdminMenu>('dashboard')
  const [analysisPeriod, setAnalysisPeriod] = useState<AnalysisPeriod | null>(null)
  const [adminError, setAdminError] = useState({ message: '', fields: {} })
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  // Rafraichi automatiquement : un dossier complete par un compte apparait sans recharger la page.
  const usersQuery = useQuery({ queryKey: ['admin', 'users'], queryFn: adminApi.users, refetchInterval: 10_000 })
  const providersQuery = useQuery({ queryKey: ['admin', 'providers'], queryFn: () => adminApi.providers() })
  const bookingsQuery = useQuery({ queryKey: ['admin', 'bookings'], queryFn: () => adminApi.bookings() })
  const contactMessagesQuery = useQuery({ queryKey: ['admin', 'contact-messages'], queryFn: adminApi.contactMessages })

  const providerStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; providerName: string; status: ProviderStatus }) => adminApi.updateProviderStatus(id, status),
    onMutate: () => setAdminError({ message: '', fields: {} }),
    onSuccess: (_, variables) => {
      toast.success(
        variables.status === 'APPROVED' ? 'Profil prestataire valide' : 'Profil prestataire rejete',
        `Le profil de ${variables.providerName} a ete ${variables.status === 'APPROVED' ? 'valide' : 'rejete'}.`,
      )
      void queryClient.invalidateQueries({ queryKey: ['admin', 'providers'] })
      void queryClient.invalidateQueries({ queryKey: ['providers'] })
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setAdminError(apiError)
      toast.error('Mise a jour du profil impossible', apiError.message)
    },
  })
  const createUserMutation = useMutation({
    mutationFn: (payload: AdminCreateUserPayload) => adminApi.createUser(payload),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
  const userStatusMutation = useMutation({
    mutationFn: ({ id, active }: { active: boolean; id: string; userName: string }) => adminApi.updateUserActive(id, active),
    onMutate: () => setAdminError({ message: '', fields: {} }),
    onSuccess: (_, variables) => {
      toast.success(
        variables.active ? 'Compte valide' : 'Compte invalide',
        `Le compte de ${variables.userName} a ete ${variables.active ? 'valide' : 'invalide'}.`,
      )
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      void queryClient.invalidateQueries({ queryKey: ['providers'] })
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setAdminError(apiError)
      toast.error('Mise a jour du compte impossible', apiError.message)
    },
  })
  const userArchiveMutation = useMutation({
    mutationFn: ({ id, archived }: { archived: boolean; id: string; userName: string }) => adminApi.updateUserArchived(id, archived),
    onMutate: () => setAdminError({ message: '', fields: {} }),
    onSuccess: (_, variables) => {
      toast.success(
        variables.archived ? 'Compte archive' : 'Compte restaure',
        `Le compte de ${variables.userName} a ete ${variables.archived ? 'archive' : 'restaure'}.`,
      )
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      void queryClient.invalidateQueries({ queryKey: ['providers'] })
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setAdminError(apiError)
      toast.error('Archivage du compte impossible', apiError.message)
    },
  })
  const bookingStatusMutation = useMutation({
    mutationFn: ({ id, status }: { bookingReference: string; id: string; status: BookingStatus }) => adminApi.updateBookingStatus(id, status),
    onSuccess: (_, variables) => {
      toast.success('Reservation mise a jour', `${variables.bookingReference} est maintenant "${formatStatus(variables.status)}".`)
      void queryClient.invalidateQueries({ queryKey: ['admin', 'bookings'] })
    },
    onError: (caughtError) => toast.error('Reservation non mise a jour', getApiError(caughtError).message),
  })
  const exportContactMessagesMutation = useMutation({
    mutationFn: (format: 'csv' | 'excel') => adminApi.exportContactMessages(format).then((blob) => ({ blob, format })),
    onSuccess: ({ blob, format }) => {
      downloadBlob(blob, `messages-contact-jaankha.${format === 'excel' ? 'xls' : 'csv'}`)
      toast.success('Export pret', `Le fichier ${format === 'excel' ? 'Excel' : 'CSV'} a ete telecharge.`)
    },
    onError: (caughtError) => toast.error('Export impossible', getApiError(caughtError).message),
  })

  const isSuperAdmin = isSuperAdminRole(user?.role)
  const manageableUsers = usersQuery.data?.filter((currentUser) => currentUser.role === 'EMPLOYEUR' || currentUser.role === 'PRESTATAIRE') ?? []
  // Comptes d'administration : renvoyes par l'API uniquement au super administrateur.
  const administrationUsers = usersQuery.data?.filter((currentUser) => isAdministrationRole(currentUser.role)) ?? []
  const providers = providersQuery.data ?? []
  const bookings = bookingsQuery.data ?? []
  const pendingBookings = bookings.filter((booking) => booking.status === 'REQUESTED')
  const contactMessages = contactMessagesQuery.data ?? []

  // Indicateurs du dashboard, recalcules selon la periode d'analyse choisie.
  // (Les badges du menu restent globaux : ils signalent tout ce qui attend une action.)
  const periodMetrics = useMemo(() => {
    const range = analysisPeriod ? periodRange(analysisPeriod) : null
    const allUsers = usersQuery.data ?? []
    const allBookings = bookingsQuery.data ?? []
    const allProviders = providersQuery.data ?? []
    // Seuls les employeurs et prestataires sont comptes : le chiffre est le meme pour un admin et le super admin.
    const usersInPeriod = allUsers
      .filter((account) => !isAdministrationRole(account.role))
      .filter((account) => isWithinPeriod(account.createdAt, range))
    const userIdsInPeriod = new Set(usersInPeriod.map((account) => account.id))
    const bookingsInPeriod = allBookings.filter((booking) => isWithinPeriod(booking.createdAt, range))
    return {
      caption: range ? range.label : 'Depuis le début',
      users: usersInPeriod.length,
      pendingProfiles: countPendingProfileApprovals(
        usersInPeriod,
        // Un profil prestataire est rattache a la periode d'inscription de son compte.
        allProviders.filter((provider) => userIdsInPeriod.has(provider.userId)),
      ),
      bookings: bookingsInPeriod.length,
      pendingBookings: bookingsInPeriod.filter((booking) => booking.status === 'REQUESTED').length,
    }
  }, [analysisPeriod, usersQuery.data, bookingsQuery.data, providersQuery.data])
  const adminItems: WorkspaceNavItem<AdminMenu>[] = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: <DashboardIcon />,
    },
    {
      id: 'accounts',
      label: 'Gestion des comptes',
      icon: <UsersIcon />,
      badge: manageableUsers.length.toLocaleString('fr-FR'),
    },
    {
      id: 'bookings',
      label: 'Gestion des reservations',
      icon: <BagIcon />,
      badge: pendingBookings.length.toLocaleString('fr-FR'),
    },
    ...(isSuperAdmin
      ? [{ id: 'history' as const, label: 'Historique des actions', icon: <HistoryIcon /> }]
      : []),
    {
      id: 'contact',
      label: 'Messages contact',
      icon: <MailIcon />,
      badge: contactMessages.length.toLocaleString('fr-FR'),
    },
    {
      id: 'profile',
      label: 'Mon profil',
      icon: <ProfileIcon />,
    },
  ]

  return (
    <WorkspaceShell
      activeItem={activeMenu}
      eyebrow="Back office"
      items={adminItems}
      title="Pilotage Jaankha"
      userContact={user?.email || user?.phone || undefined}
      userName={user?.fullName || 'Administrateur'}
      onSelect={setActiveMenu}
    >
      {activeMenu === 'dashboard' && (
        <>
          <PeriodFilter value={analysisPeriod} onChange={setAnalysisPeriod} />
          <div className="booking-metrics-grid admin-overview-metrics">
            <BookingMetricCard icon={<UsersIcon />} label="Utilisateurs" value={periodMetrics.users.toLocaleString('fr-FR')} caption={periodMetrics.caption} />
            <BookingMetricCard icon={<ProfilePendingIcon />} label="Profils en attente" value={periodMetrics.pendingProfiles.toLocaleString('fr-FR')} caption={periodMetrics.caption} />
            <BookingMetricCard icon={<BagIcon />} label="Reservation totale" value={periodMetrics.bookings.toLocaleString('fr-FR')} caption={periodMetrics.caption} />
            <BookingMetricCard icon={<HourglassIcon />} label="Reservation en attente" value={periodMetrics.pendingBookings.toLocaleString('fr-FR')} caption={periodMetrics.caption} />
          </div>
          <DashboardOverview bookings={bookings} providers={providers} users={usersQuery.data ?? []} period={analysisPeriod} />
        </>
      )}
      {usersQuery.isError && <p className="empty-state">Impossible de charger les utilisateurs.</p>}
      {providersQuery.isError && <p className="empty-state">Impossible de charger les profils prestataires.</p>}
      {contactMessagesQuery.isError && <p className="empty-state">Impossible de charger les messages contact.</p>}
      <FormError message={adminError.message} fields={adminError.fields} />
      {activeMenu === 'accounts' && (
        <div className="dashboard-grid">
          <section className="panel span-2">
            <h2>Creer un compte</h2>
            <CreateUserForm allowAdmin={isSuperAdmin} loading={createUserMutation.isPending} onCreate={(payload) => createUserMutation.mutateAsync(payload)} />
            <CsvUsersImport loading={createUserMutation.isPending} onCreate={(payload) => createUserMutation.mutateAsync(payload)} />
          </section>
          {isSuperAdmin && (
            <section className="panel span-2">
              <h2>Administrateurs</h2>
              <p className="muted">Comptes administrateur que tu as crees : tu peux les desactiver, les reactiver ou les archiver.</p>
              {administrationUsers.length === 0 ? (
                <p className="empty-state">Aucun compte administrateur pour le moment.</p>
              ) : (
                <AccountsTable
                  canManageAdmins
                  loading={userStatusMutation.isPending || userArchiveMutation.isPending}
                  users={administrationUsers}
                  onToggle={(selectedAccount, active) => userStatusMutation.mutate({ id: selectedAccount.id, active, userName: selectedAccount.fullName })}
                  onArchive={(selectedAccount, archived) => userArchiveMutation.mutate({ id: selectedAccount.id, archived, userName: selectedAccount.fullName })}
                  onInspect={setSelectedUser}
                />
              )}
            </section>
          )}
          <section className="panel span-2">
            <h2>Gestion des comptes</h2>
            {manageableUsers.length === 0 ? (
              <p className="empty-state">Aucun compte employeur ou prestataire pour le moment.</p>
            ) : (
              <AccountsTable
                loading={userStatusMutation.isPending || userArchiveMutation.isPending}
                users={manageableUsers}
                onToggle={(selectedAccount, active) => userStatusMutation.mutate({ id: selectedAccount.id, active, userName: selectedAccount.fullName })}
                onArchive={(selectedAccount, archived) => userArchiveMutation.mutate({ id: selectedAccount.id, archived, userName: selectedAccount.fullName })}
                onInspect={setSelectedUser}
              />
            )}
            {selectedUser && <UserDetailPanel user={selectedUser} onClose={() => setSelectedUser(null)} />}
          </section>
          <ProviderProfilesReview
            providers={providers}
            onStatusChange={(provider, status) => providerStatusMutation.mutate({ id: provider.id, providerName: provider.fullName, status })}
            onInspect={(provider) => setSelectedUser(usersQuery.data?.find((account) => account.id === provider.userId) ?? null)}
          />
        </div>
      )}
      {activeMenu === 'bookings' && (
        <BookingsManagement
          bookings={bookings}
          loading={bookingStatusMutation.isPending}
          onStatusChange={(booking, status) => bookingStatusMutation.mutate({ id: booking.id, bookingReference: bookingReference(booking.id), status })}
        />
      )}
      {activeMenu === 'profile' && <AdminProfilePanel />}
      {activeMenu === 'history' && isSuperAdmin && (
        <div className="dashboard-grid">
          <AdminAuditLog users={usersQuery.data ?? []} />
        </div>
      )}
      {activeMenu === 'contact' && (
        <div className="dashboard-grid">
          <AdminContactMessages
            exporting={exportContactMessagesMutation.isPending}
            loading={contactMessagesQuery.isLoading}
            messages={contactMessages}
            onExport={(format) => exportContactMessagesMutation.mutate(format)}
          />
        </div>
      )}
    </WorkspaceShell>
  )
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
