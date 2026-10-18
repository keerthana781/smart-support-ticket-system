// Small coloured labels used across the app, plus human-friendly text for enum values.

export const STATUS_LABELS = {
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
}

export const PRIORITY_LABELS = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', URGENT: 'Urgent' }

export const CATEGORY_LABELS = {
  BILLING: 'Billing',
  TECHNICAL: 'Technical',
  ACCOUNT: 'Account',
  FEATURE_REQUEST: 'Feature request',
  GENERAL: 'General',
}

export function StatusBadge({ status }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{STATUS_LABELS[status] || status}</span>
}

export function PriorityBadge({ priority }) {
  return (
    <span className={`badge priority-${priority.toLowerCase()}`}>
      <span className="dot" aria-hidden="true" />
      {PRIORITY_LABELS[priority] || priority}
    </span>
  )
}

export function CategoryBadge({ category }) {
  return <span className="badge category">{CATEGORY_LABELS[category] || category}</span>
}

export function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function timeAgo(iso) {
  if (!iso) return ''
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return `${days}d ago`
}
