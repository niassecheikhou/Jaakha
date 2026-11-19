import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useToast } from '../../components/toast-context'
import { getApiError, notificationsApi } from '../../services/api'

export function NotificationsPanel() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const notificationsQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: notificationsApi.list,
  })
  const readMutation = useMutation({
    mutationFn: notificationsApi.markRead,
    onSuccess: () => {
      toast.success('Notification mise a jour', 'La notification a ete marquee comme lue.')
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (caughtError) => toast.error('Notification non mise a jour', getApiError(caughtError).message),
  })
  const unreadNotifications = notificationsQuery.data?.filter((notification) => !notification.read) ?? []

  return (
    <section className="panel">
      <h2>Notifications</h2>
      {unreadNotifications.length === 0 && <p className="empty-state">Aucune notification.</p>}
      <div className="stack">
        {unreadNotifications.slice(0, 6).map((notification) => (
          <article className={notification.read ? 'notice read' : 'notice'} key={notification.id}>
            <strong>{notification.title}</strong>
            <p>{notification.message}</p>
            <button className="link-button" type="button" onClick={() => readMutation.mutate(notification.id)}>
              Marquer comme lue
            </button>
          </article>
        ))}
      </div>
    </section>
  )
}
