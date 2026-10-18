import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'

export default function NewTicketPage() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setFieldErrors({})
    setSubmitting(true)
    try {
      const ticket = await api.createTicket(title, description)
      navigate(`/tickets/${ticket.id}`, { state: { justCreated: true } })
    } catch (err) {
      setError(err.message)
      setFieldErrors(err.fieldErrors || {})
      setSubmitting(false)
    }
  }

  return (
    <div className="page page-narrow">
      <div className="page-header">
        <div>
          <h1>Raise a ticket</h1>
          <p className="muted">Describe the problem. We&apos;ll categorise and prioritise it automatically.</p>
        </div>
      </div>

      <form className="card form" onSubmit={submit}>
        <label className="field">
          <span>Title</span>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Payment deducted but order not confirmed"
            maxLength={150}
            required
            autoFocus
          />
          {fieldErrors.title && <small className="field-error">{fieldErrors.title}</small>}
        </label>

        <label className="field">
          <span>Description</span>
          <textarea
            rows={7}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="What happened? What did you expect? Include any error messages, dates or order numbers."
            maxLength={5000}
            required
          />
          <small className={fieldErrors.description ? 'field-error' : 'muted'}>
            {fieldErrors.description || `${description.length} / 5000`}
          </small>
        </label>

        {error && !Object.keys(fieldErrors).length && <div className="alert alert-error">{error}</div>}

        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={() => navigate(-1)}>Cancel</button>
          <button className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Submitting & analysing…' : 'Submit ticket'}
          </button>
        </div>
      </form>
    </div>
  )
}
