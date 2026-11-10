
import { useEffect, useState, type ReactNode } from 'react'
import { loginApi, getCurrentUserApi } from '../services/authApi'
import type { CurrentUser } from '../types/auth'
import { AuthContext } from './AuthContextValue'

const TOKEN_KEY = 'hoakhanh_access_token'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => {
    return sessionStorage.getItem(TOKEN_KEY)
  })

  const [user, setUser] = useState<CurrentUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function restoreSession() {
      if (!token) {
        setLoading(false)
        return
      }

      try {
        const currentUser = await getCurrentUserApi(token)

        if (!cancelled) {
          setUser(currentUser)
          setLoading(false)
        }
      } catch {
        if (!cancelled) {
          sessionStorage.removeItem(TOKEN_KEY)
          setToken(null)
          setUser(null)
          setLoading(false)
        }
      }
    }

    void restoreSession()

    return () => {
      cancelled = true
    }
  }, [token])

  async function login(email: string, password: string) {
    const response = await loginApi({ email, password })

    const currentUser = await getCurrentUserApi(
      response.accessToken
    )

    sessionStorage.setItem(TOKEN_KEY, response.accessToken)
    setToken(response.accessToken)
    setUser(currentUser)
    setLoading(false)
  }

  function logout() {
    sessionStorage.removeItem(TOKEN_KEY)
    setToken(null)
    setUser(null)
    setLoading(false)
  }

  return (
    <AuthContext.Provider
      value={{ user, token, loading, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}
