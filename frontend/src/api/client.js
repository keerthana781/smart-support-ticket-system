// Small wrapper around fetch: adds the JWT, parses JSON and turns API errors into exceptions.

const TOKEN_KEY = 'supportdesk.token'

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    // storage unavailable (private mode): the user just has to log in again next time
  }
}

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?')
  }

  const text = await response.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    // not JSON (for example a proxy error page)
  }

  if (!response.ok) {
    if (response.status === 401 && token) {
      // Token expired or invalid: tell the app to log out.
      window.dispatchEvent(new Event('supportdesk:unauthorized'))
    }
    throw new ApiError(response.status, data?.message || 'Request failed', data?.fieldErrors || {})
  }
  return data
}

function query(params) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '' && value !== false) search.set(key, value)
  })
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}

export const api = {
  login: (email, password) => request('POST', '/auth/login', { email, password }),
  register: (fullName, email, password) => request('POST', '/auth/register', { fullName, email, password }),
  me: () => request('GET', '/users/me'),
  staff: () => request('GET', '/users/staff'),

  listTickets: (filters) => request('GET', `/tickets${query(filters)}`),
  getTicket: (id) => request('GET', `/tickets/${id}`),
  createTicket: (title, description) => request('POST', '/tickets', { title, description }),
  changeStatus: (id, status) => request('PATCH', `/tickets/${id}/status`, { status }),
  updateTicket: (id, changes) => request('PATCH', `/tickets/${id}`, changes),
  assignToMe: (id) => request('POST', `/tickets/${id}/assign-to-me`),
  addComment: (id, body, internal, aiAssisted) =>
    request('POST', `/tickets/${id}/comments`, { body, internal, aiAssisted }),
  draftReply: (id) => request('POST', `/tickets/${id}/ai/draft-reply`),

  adminUsers: () => request('GET', '/admin/users'),
  adminUpdateUser: (id, changes) => request('PATCH', `/admin/users/${id}`, changes),
  adminStats: () => request('GET', '/admin/stats'),
}
