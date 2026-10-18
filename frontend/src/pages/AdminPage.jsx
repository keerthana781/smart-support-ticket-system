import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { CATEGORY_LABELS, PRIORITY_LABELS, STATUS_LABELS, formatDate } from '../components/Badges.jsx'

export default function AdminPage() {
  const { user: me } = useAuth()
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  const [error, setError] = useState('')

  const load = () => {
    api.adminStats().then(setStats).catch((err) => setError(err.message))
    api.adminUsers().then(setUsers).catch((err) => setError(err.message))
  }

  useEffect(load, [])

  const updateUser = async (id, changes) => {
    setError('')
    try {
      const updated = await api.adminUpdateUser(id, changes)
      setUsers((list) => list.map((u) => (u.id === id ? updated : u)))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Admin</h1>
          <p className="muted">Workload overview, AI quality and user management.</p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {stats && (
        <>
          <div className="stat-grid">
            <Stat label="Total tickets" value={stats.totalTickets} />
            <Stat label="Open" value={stats.byStatus.OPEN ?? 0} />
            <Stat label="In progress" value={stats.byStatus.IN_PROGRESS ?? 0} />
            <Stat
              label="AI agreement"
              value={stats.aiTriagedTickets ? `${stats.aiAgreementRate}%` : '—'}
              hint={`${stats.aiTriagedTickets - stats.aiOverriddenTickets} of ${stats.aiTriagedTickets} kept as suggested · engine: ${stats.aiProvider === 'GEMINI' ? 'Gemini' : 'rules'}`}
            />
          </div>

          <div className="breakdown-grid">
            <Breakdown title="By status" data={stats.byStatus} labels={STATUS_LABELS} total={stats.totalTickets} />
            <Breakdown title="By priority" data={stats.byPriority} labels={PRIORITY_LABELS} total={stats.totalTickets} />
            <Breakdown title="By category" data={stats.byCategory} labels={CATEGORY_LABELS} total={stats.totalTickets} />
          </div>
        </>
      )}

      <div className="card table-card">
        <h2 className="section-title table-title">Users</h2>
        <table className="ticket-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const self = u.id === me.id
              return (
                <tr key={u.id}>
                  <td>{u.fullName}{self && <span className="muted"> (you)</span>}</td>
                  <td className="muted">{u.email}</td>
                  <td>
                    <select value={u.role} disabled={self} onChange={(e) => updateUser(u.id, { role: e.target.value })}>
                      <option value="CUSTOMER">Customer</option>
                      <option value="AGENT">Agent</option>
                      <option value="ADMIN">Admin</option>
                    </select>
                  </td>
                  <td>
                    <button
                      className={`btn btn-sm ${u.active ? 'btn-secondary' : 'btn-success'}`}
                      disabled={self}
                      onClick={() => updateUser(u.id, { active: !u.active })}
                    >
                      {u.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                  <td className="muted">{formatDate(u.createdAt)}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function Stat({ label, value, hint }) {
  return (
    <div className="card stat">
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  )
}

function Breakdown({ title, data, labels, total }) {
  return (
    <div className="card">
      <h2 className="section-title">{title}</h2>
      <ul className="bars">
        {Object.keys(labels).map((key) => {
          const count = data[key] ?? 0
          const pct = total ? Math.round((count / total) * 100) : 0
          return (
            <li key={key}>
              <div className="bar-label">
                <span>{labels[key]}</span>
                <span className="muted">{count}</span>
              </div>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${pct}%` }} />
              </div>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
