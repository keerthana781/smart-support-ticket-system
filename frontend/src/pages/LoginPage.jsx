import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

// Demo accounts created by the backend's DataSeeder. Handy for anyone reviewing the project.
const DEMO_ACCOUNTS = [
  { label: 'Customer', email: 'customer@demo.com', password: 'Customer@123' },
  { label: 'Agent', email: 'agent@demo.com', password: 'Agent@123' },
  { label: 'Admin', email: 'admin@demo.com', password: 'Admin@123' },
]

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <h1>Welcome back</h1>
          <p className="muted">Sign in to SupportDesk</p>
        </div>

        <form onSubmit={submit} className="form">
          <label className="field">
            <span>Email</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          {error && <div className="alert alert-error">{error}</div>}
          <button className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="demo-box">
          <div className="demo-title">Try a demo account</div>
          <div className="demo-buttons">
            {DEMO_ACCOUNTS.map((acc) => (
              <button
                key={acc.email}
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setEmail(acc.email)
                  setPassword(acc.password)
                }}
              >
                {acc.label}
              </button>
            ))}
          </div>
        </div>

        <p className="auth-switch">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  )
}
