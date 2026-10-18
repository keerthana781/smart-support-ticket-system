import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import {
  CATEGORY_LABELS,
  CategoryBadge,
  PRIORITY_LABELS,
  PriorityBadge,
  STATUS_LABELS,
  StatusBadge,
  timeAgo,
} from '../components/Badges.jsx'

const EMPTY_FILTERS = { status: '', priority: '', category: '', q: '', assignedToMe: false }

export default function TicketListPage() {
  const { isStaff } = useAuth()
  const navigate = useNavigate()
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  // Debounce the search box so we don't call the API on every keystroke.
  useEffect(() => {
    const timer = setTimeout(() => {
      setFilters((f) => ({ ...f, q: search }))
      setPage(0)
    }, 350)
    return () => clearTimeout(timer)
  }, [search])

  useEffect(() => {
    let cancelled = false
    setError('')
    api.listTickets({ ...filters, page, size: 15 })
      .then((data) => !cancelled && setResult(data))
      .catch((err) => !cancelled && setError(err.message))
    return () => {
      cancelled = true
    }
  }, [filters, page])

  const setFilter = (key) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value
    setFilters({ ...filters, [key]: value })
    setPage(0)
  }

  const hasFilters = Object.entries(filters).some(([, v]) => v) || search

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>{isStaff ? 'All tickets' : 'My tickets'}</h1>
          <p className="muted">
            {isStaff ? 'Triage, assign and resolve customer issues.' : 'Track the status of your support requests.'}
          </p>
        </div>
        <Link to="/tickets/new" className="btn btn-primary">+ New ticket</Link>
      </div>

      <div className="filters card">
        <input
          className="search"
          placeholder="Search title or description…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select value={filters.status} onChange={setFilter('status')} aria-label="Status">
          <option value="">All statuses</option>
          {Object.entries(STATUS_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select value={filters.priority} onChange={setFilter('priority')} aria-label="Priority">
          <option value="">All priorities</option>
          {Object.entries(PRIORITY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select value={filters.category} onChange={setFilter('category')} aria-label="Category">
          <option value="">All categories</option>
          {Object.entries(CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        {isStaff && (
          <label className="checkbox">
            <input type="checkbox" checked={filters.assignedToMe} onChange={setFilter('assignedToMe')} />
            Assigned to me
          </label>
        )}
        {hasFilters && (
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => {
              setFilters(EMPTY_FILTERS)
              setSearch('')
            }}
          >
            Clear
          </button>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {!result && !error && <div className="card empty">Loading tickets…</div>}

      {result && result.content.length === 0 && (
        <div className="card empty">
          <p>No tickets found.</p>
          {!hasFilters && <Link to="/tickets/new" className="btn btn-primary btn-sm">Raise your first ticket</Link>}
        </div>
      )}

      {result && result.content.length > 0 && (
        <div className="card table-card">
          <table className="ticket-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Title</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Category</th>
                {isStaff && <th>Customer</th>}
                <th>Assignee</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {result.content.map((t) => (
                <tr key={t.id} onClick={() => navigate(`/tickets/${t.id}`)} className="clickable">
                  <td className="muted mono">#{t.id}</td>
                  <td className="title-cell">
                    <Link to={`/tickets/${t.id}`} onClick={(e) => e.stopPropagation()}>{t.title}</Link>
                  </td>
                  <td><StatusBadge status={t.status} /></td>
                  <td><PriorityBadge priority={t.priority} /></td>
                  <td><CategoryBadge category={t.category} /></td>
                  {isStaff && <td>{t.createdBy.fullName}</td>}
                  <td>{t.assignedTo ? t.assignedTo.fullName : <span className="muted">Unassigned</span>}</td>
                  <td className="muted">{timeAgo(t.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {result.totalPages > 1 && (
            <div className="pagination">
              <button className="btn btn-secondary btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
                Previous
              </button>
              <span className="muted">
                Page {result.page + 1} of {result.totalPages} · {result.totalElements} tickets
              </span>
              <button
                className="btn btn-secondary btn-sm"
                disabled={page + 1 >= result.totalPages}
                onClick={() => setPage(page + 1)}
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
