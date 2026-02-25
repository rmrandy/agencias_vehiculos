import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import type { User } from '../api/auth'

const STORAGE_KEY = 'distribuidores_user'

function getStoredUser(): User | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

type AuthContextType = {
  user: User | null
  setUser: (u: User | null) => void
  isLoggedIn: boolean
  isEnterprise: boolean
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<User | null>(getStoredUser)

  useEffect(() => {
    if (user) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }, [user])

  const setUser = (u: User | null) => setUserState(u)
  const isLoggedIn = !!user
  const isEnterprise = !!(user?.roles && user.roles.includes('ENTERPRISE'))

  const logout = () => {
    setUserState(null)
  }

  return (
    <AuthContext.Provider value={{ user, setUser, isLoggedIn, isEnterprise, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
