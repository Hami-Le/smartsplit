import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  login as loginRequest,
  register as registerRequest,
  type AuthData,
  type AuthUser,
  type LoginInput,
  type RegisterInput,
} from '../api/authApi'

const AUTH_STORAGE_KEY = 'smartsplit.auth'

type AuthContextValue = {
  user: AuthUser | null
  accessToken: string | null
  isAuthenticated: boolean
  login: (input: LoginInput) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function readStoredAuth(): AuthData | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthData
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthData | null>(() => readStoredAuth())

  useEffect(() => {
    const handleExpiredSession = () => {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      setAuth(null)
      sessionStorage.setItem('smartsplit.sessionMessage', 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
      window.history.replaceState({}, '', '/login')
      window.dispatchEvent(new PopStateEvent('popstate'))
    }
    window.addEventListener('smartsplit:auth-expired', handleExpiredSession)
    return () => window.removeEventListener('smartsplit:auth-expired', handleExpiredSession)
  }, [])

  const persist = (data: AuthData) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(data))
    setAuth(data)
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      user: auth?.user ?? null,
      accessToken: auth?.accessToken ?? null,
      isAuthenticated: Boolean(auth?.accessToken),
      login: async (input) => persist(await loginRequest(input)),
      register: async (input) => persist(await registerRequest(input)),
      logout: () => {
        localStorage.removeItem(AUTH_STORAGE_KEY)
        setAuth(null)
      },
    }),
    [auth],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext<AuthContextValue | null>(AuthContext)
  if (!context) {
    throw new Error('useAuth phải được dùng bên trong AuthProvider')
  }
  return context
}
