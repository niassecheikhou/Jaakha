import type { ReactNode } from 'react'

export type WorkspaceNavItem<T extends string> = {
  id: T
  label: string
  icon: ReactNode
  badge?: string
}

type WorkspaceShellProps<T extends string> = {
  title: string
  eyebrow: string
  userName: string
  userContact?: string
  items: WorkspaceNavItem<T>[]
  activeItem: T
  children: ReactNode
  onSelect: (item: T) => void
}

export function WorkspaceShell<T extends string>({
  title,
  eyebrow,
  userName,
  userContact,
  items,
  activeItem,
  children,
  onSelect,
}: WorkspaceShellProps<T>) {
  return (
    <section className="workspace-shell">
      <aside className="workspace-sidebar" aria-label={`Navigation ${title}`}>
        <div className="workspace-user-card">
          <strong>{userName}</strong>
          {userContact && <span>{userContact}</span>}
        </div>
        <nav className="workspace-nav">
          {items.map((item) => (
            <button
              aria-current={activeItem === item.id ? 'page' : undefined}
              className={activeItem === item.id ? 'active' : ''}
              key={item.id}
              type="button"
              onClick={() => onSelect(item.id)}
            >
              <span className="workspace-nav-icon">{item.icon}</span>
              <span>{item.label}</span>
              {item.badge && <strong>{item.badge}</strong>}
            </button>
          ))}
        </nav>
      </aside>
      <div className="workspace-content">
        <div className="section-heading workspace-heading">
          <div>
            <p className="eyebrow">{eyebrow}</p>
            <h1>{title}</h1>
          </div>
        </div>
        {children}
      </div>
    </section>
  )
}
