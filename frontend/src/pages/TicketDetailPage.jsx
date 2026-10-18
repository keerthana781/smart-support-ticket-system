import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import {
  CATEGORY_LABELS,
  CategoryBadge,
  PRIORITY_LABELS,
  PriorityBadge,
  STATUS_LABELS,
  StatusBadge,
  formatDate,
} from '../components/Badges.jsx'

// Button text for each status move, from the user's point of view.
const STATUS_ACTIONS = {
  OPEN: { label: 'Reopen', style: 'btn-secondary' },
  IN_PROGRESS: { label: 'Start work', style: 'btn-primary' },
  RESOLVED: { label: 'Mark resolved', style: 'btn-success' },
  CLOSED: { label: 'Close ticket', style: 'btn-secondary' },
}

export default function TicketDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const { user, isStaff } = useAuth()

  const [ticket, setTicket] = useState(null)
  const [staff, setStaff] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState(location.state?.justCreated ? 'created' : '')

  // Reply composer state
  const [reply, setReply] = useState('')
  const [internal, setInternal] = useState(false)
  const [aiAssisted, setAiAssisted] = useState(false)
  const [drafting, setDrafting] = useState(false)

  useEffect(() => {
    setTicket(null)
    setError('')
    api.getTicket(id).then(setTicket).catch((err) => setError(err.message))
  }, [id])

  useEffect(() => {
    if (isStaff) api.staff().then(setStaff).catch(() => setStaff([]))
  }, [isStaff])

  // Runs an API call that returns the updated ticket, with shared loading/error handling.
  const run = async (action) => {
    setBusy(true)
    setError('')
    try {
      setTicket(await action())
      return true
    } catch (err) {
      setError(err.message)
      return false
    } finally {
      setBusy(false)
    }
  }

  const applyAiSuggestion = () => {
    setReply(ticket.ai.suggestedReply)
    setAiAssisted(true)
    setInternal(false)
  }

  const generateDraft = async () => {
    setDrafting(true)
    setError('')
    try {
      const draft = await api.draftReply(ticket.id)
      setReply(draft.reply)
      setAiAssisted(true)
      setInternal(false)
    } catch (err) {
      setError(err.message)
    } finally {
      setDrafting(false)
    }
  }

  const sendReply = async (e) => {
    e.preventDefault()
    if (!reply.trim()) return
    const ok = await run(() => api.addComment(ticket.id, reply, internal, aiAssisted))
    if (ok) {
      setReply('')
      setInternal(false)
      setAiAssisted(false)
    }
  }

  if (error && !ticket) {
    return (
      <div className="page">
        <div className="alert alert-error">{error}</div>
        <Link to="/">← Back to tickets</Link>
      </div>
    )
  }
  if (!ticket) return <div className="page-loading">Loading ticket…</div>

  const closed = ticket.status === 'CLOSED'
  const isMine = ticket.assignedTo?.id === user.id

  return (
    <div className="page">
      <Link to="/" className="back-link">← All tickets</Link>

      {notice === 'created' && (
        <div className="alert alert-success" role="status">
          Ticket submitted! It was categorised as <strong>{CATEGORY_LABELS[ticket.category]}</strong> with{' '}
          <strong>{PRIORITY_LABELS[ticket.priority].toLowerCase()}</strong> priority. Our team will reply here.
          <button className="alert-close" onClick={() => setNotice('')} aria-label="Dismiss">×</button>
        </div>
      )}

      <div className="detail-grid">
        {/* ---------- Main column ---------- */}
        <section className="detail-main">
          <div className="card">
            <div className="ticket-heading">
              <span className="muted mono">#{ticket.id}</span>
              <h1>{ticket.title}</h1>
              <div className="badge-row">
                <StatusBadge status={ticket.status} />
                <PriorityBadge priority={ticket.priority} />
                <CategoryBadge category={ticket.category} />
              </div>
              <p className="muted small">
                Opened by {ticket.createdBy.fullName} · {formatDate(ticket.createdAt)}
              </p>
            </div>
            <div className="description">{ticket.description}</div>
          </div>

          <div className="card">
            <h2 className="section-title">Conversation</h2>
            {ticket.comments.length === 0 && (
              <p className="muted">No replies yet.{!isStaff && ' Our support team will respond here.'}</p>
            )}
            <ol className="thread">
              {ticket.comments.map((c) => {
                const fromStaff = c.author.role !== 'CUSTOMER'
                return (
                  <li
                    key={c.id}
                    className={`message ${fromStaff ? 'from-staff' : 'from-customer'} ${c.internal ? 'internal' : ''}`}
                  >
                    <div className="message-meta">
                      <span className="avatar" aria-hidden="true">{c.author.fullName.charAt(0)}</span>
                      <strong>{c.author.fullName}</strong>
                      {fromStaff && <span className="muted small">Support</span>}
                      {c.internal && <span className="tag tag-internal">Internal note</span>}
                      {isStaff && c.aiAssisted && <span className="tag tag-ai">AI-assisted</span>}
                      <span className="muted small push-right">{formatDate(c.createdAt)}</span>
                    </div>
                    <div className="message-body">{c.body}</div>
                  </li>
                )
              })}
            </ol>

            {closed ? (
              <p className="muted closed-note">This ticket is closed. Raise a new ticket if you need more help.</p>
            ) : (
              <form className="composer" onSubmit={sendReply}>
                {isStaff && (
                  <div className="composer-tools">
                    {ticket.ai?.suggestedReply && (
                      <button type="button" className="btn btn-ai btn-sm" onClick={applyAiSuggestion} disabled={drafting}>
                        ✦ Use AI first reply
                      </button>
                    )}
                    <button type="button" className="btn btn-ai btn-sm" onClick={generateDraft} disabled={drafting}>
                      {drafting ? 'Drafting…' : '✦ Draft reply from conversation'}
                    </button>
                  </div>
                )}
                <textarea
                  rows={5}
                  value={reply}
                  onChange={(e) => setReply(e.target.value)}
                  placeholder={isStaff ? 'Write a reply to the customer, or an internal note…' : 'Add more details or reply to support…'}
                  className={internal ? 'internal-input' : ''}
                />
                {aiAssisted && (
                  <p className="ai-hint">
                    Drafted with AI. Please review and edit before sending: you are responsible for what the customer receives.
                  </p>
                )}
                <div className="composer-actions">
                  {isStaff && (
                    <label className="checkbox">
                      <input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} />
                      Internal note (hidden from customer)
                    </label>
                  )}
                  <button className="btn btn-primary push-right" disabled={busy || !reply.trim()}>
                    {internal ? 'Add note' : 'Send reply'}
                  </button>
                </div>
              </form>
            )}
            {error && <div className="alert alert-error">{error}</div>}
          </div>
        </section>

        {/* ---------- Sidebar ---------- */}
        <aside className="detail-side">
          {ticket.allowedNextStatuses.length > 0 && (
            <div className="card">
              <h2 className="section-title">Actions</h2>
              <div className="action-stack">
                {ticket.allowedNextStatuses.map((next) => (
                  <button
                    key={next}
                    className={`btn ${STATUS_ACTIONS[next].style}`}
                    disabled={busy}
                    onClick={() => run(() => api.changeStatus(ticket.id, next))}
                  >
                    {!isStaff && ticket.status === 'RESOLVED' && next === 'CLOSED'
                      ? 'Confirm it’s fixed'
                      : STATUS_ACTIONS[next].label}
                  </button>
                ))}
                {isStaff && !isMine && !closed && (
                  <button className="btn btn-secondary" disabled={busy} onClick={() => run(() => api.assignToMe(ticket.id))}>
                    Assign to me
                  </button>
                )}
              </div>
            </div>
          )}

          <div className="card">
            <h2 className="section-title">Details</h2>
            <dl className="details">
              <dt>Status</dt>
              <dd>{STATUS_LABELS[ticket.status]}</dd>

              <dt>Priority</dt>
              <dd>
                {isStaff && !closed ? (
                  <select
                    value={ticket.priority}
                    disabled={busy}
                    onChange={(e) => run(() => api.updateTicket(ticket.id, { priority: e.target.value }))}
                  >
                    {Object.entries(PRIORITY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                  </select>
                ) : (
                  PRIORITY_LABELS[ticket.priority]
                )}
              </dd>

              <dt>Category</dt>
              <dd>
                {isStaff && !closed ? (
                  <select
                    value={ticket.category}
                    disabled={busy}
                    onChange={(e) => run(() => api.updateTicket(ticket.id, { category: e.target.value }))}
                  >
                    {Object.entries(CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                  </select>
                ) : (
                  CATEGORY_LABELS[ticket.category]
                )}
              </dd>

              <dt>Assignee</dt>
              <dd>
                {isStaff && !closed ? (
                  <select
                    value={ticket.assignedTo?.id ?? ''}
                    disabled={busy}
                    onChange={(e) =>
                      run(() =>
                        api.updateTicket(
                          ticket.id,
                          e.target.value ? { assigneeId: Number(e.target.value) } : { unassign: true },
                        ),
                      )
                    }
                  >
                    <option value="">Unassigned</option>
                    {staff.map((s) => <option key={s.id} value={s.id}>{s.fullName}</option>)}
                  </select>
                ) : (
                  ticket.assignedTo?.fullName || 'Not yet assigned'
                )}
              </dd>

              <dt>Updated</dt>
              <dd>{formatDate(ticket.updatedAt)}</dd>

              {ticket.resolvedAt && (
                <>
                  <dt>Resolved</dt>
                  <dd>{formatDate(ticket.resolvedAt)}</dd>
                </>
              )}
            </dl>
          </div>

          {isStaff && ticket.ai && <AiPanel ai={ticket.ai} />}
        </aside>
      </div>
    </div>
  )
}

function AiPanel({ ai }) {
  return (
    <div className="card ai-card">
      <div className="ai-card-header">
        <h2 className="section-title">✦ AI triage</h2>
        <span className={`tag ${ai.source === 'GEMINI' ? 'tag-ai' : 'tag-muted'}`}>
          {ai.source === 'GEMINI' ? 'Gemini' : 'Keyword rules'}
        </span>
      </div>
      <dl className="details">
        <dt>Suggested category</dt>
        <dd>{CATEGORY_LABELS[ai.suggestedCategory]}</dd>
        <dt>Suggested priority</dt>
        <dd>{PRIORITY_LABELS[ai.suggestedPriority]}</dd>
      </dl>
      {ai.reasoning && <p className="ai-reason">“{ai.reasoning}”</p>}
      <p className={`ai-verdict ${ai.overriddenByAgent ? 'overridden' : 'kept'}`}>
        {ai.overriddenByAgent ? 'An agent changed the AI suggestion.' : 'Current values match the AI suggestion.'}
      </p>
    </div>
  )
}
