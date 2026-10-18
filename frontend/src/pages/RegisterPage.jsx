import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setFieldErrors({})
    setSubmitting(true)
    try {
      await register(form.fullName, form.email, form.password)
      navigate('/')
    } catch (err) {
      setError(err.message)
      setFieldErrors(err.fieldErrors || {})
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <h1>Create your account</h1>
          <p className="muted">Raise and track support tickets</p>
        </div>

        <form onSubmit={submit} className="form">
          <label className="field">
            <span>Full name</span>
            <input value={form.fullName} onChange={update('fullName')} required autoFocus />
            {fieldErrors.fullName && <small className="field-error">{fieldErrors.fullName}</small>}
          </label>
          <label className="field">
            <span>Email</span>
            <input type="email" value={form.email} onChange={update('email')} required />
            {fieldErrors.email && <small className="field-error">{fieldErrors.email}</small>}
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" value={form.password} onChange={update('password')} minLength={8} required />
            <small className={fieldErrors.password ? 'field-error' : 'muted'}>
              {fieldErrors.password || 'At least 8 characters'}
            </small>
          </label>
          {error && !Object.keys(fieldErrors).length && <div className="alert alert-error">{error}</div>}
          <button className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
