import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, getToken, setToken } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
  }, [])

  // On first load, check whether a saved token is still valid.
  useEffect(() => {
    if (!getToken()) {
      setLoading(false)
      return
    }
    api.me()
      .then(setUser)
      .catch(() => setToken(null))
      .finally(() => setLoading(false))
  }, [])

  // Any 401 from the API logs the user out.
  useEffect(() => {
    window.addEventListener('supportdesk:unauthorized', logout)
    return () => window.removeEventListener('supportdesk:unauthorized', logout)
  }, [logout])

  const handleAuth = (response) => {
    setToken(response.token)
    setUser(response.user)
    return response.user
  }

  const login = async (email, password) => handleAuth(await api.login(email, password))
  const register = async (fullName, email, password) => handleAuth(await api.register(fullName, email, password))

  const isStaff = user?.role === 'AGENT' || user?.role === 'ADMIN'
  const isAdmin = user?.role === 'ADMIN'

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isStaff, isAdmin }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
