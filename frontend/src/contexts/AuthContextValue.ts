
import { createContext } from 'react'
import type { CurrentUser } from '../types/auth'

export interface AuthContextType {
  user: CurrentUser | null
  token: string | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextType | undefined>(
  undefined
)
