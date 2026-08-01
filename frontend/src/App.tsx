import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { useAuth } from './auth/AuthContext'
import { AppHeader } from './components/AppHeader'
import { BalancePage } from './pages/BalancePage'
import { CreateGroupPage } from './pages/CreateGroupPage'
import { DashboardPage } from './pages/DashboardPage'
import { ExpenseDetailPage } from './pages/ExpenseDetailPage'
import { ExpenseFormPage } from './pages/ExpenseFormPage'
import { GroupDetailPage } from './pages/GroupDetailPage'
import { GroupListPage } from './pages/GroupListPage'
import { InvitationAcceptPage } from './pages/InvitationAcceptPage'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { AccountPage } from './pages/AccountPage'
import { RegisterPage } from './pages/RegisterPage'
import { currentPath, navigate } from './router'

function RedirectToLogin({ returnTo }: { returnTo: string }) {
  useEffect(() => {
    sessionStorage.setItem('smartsplit.returnTo', returnTo)
    navigate('/login', true)
  }, [returnTo])
  return null
}

function NotFoundPage() {
  return (
    <section className="empty-state standalone-empty">
      <span className="empty-icon">404</span>
      <h1>Không tìm thấy trang</h1>
      <p>Đường dẫn bạn mở không tồn tại trong SmartSplit.</p>
      <button className="button button-primary" type="button" onClick={() => navigate('/')}>Về trang chủ</button>
    </section>
  )
}

export default function App() {
  const { isAuthenticated } = useAuth()
  const [path, setPath] = useState(currentPath())

  useEffect(() => {
    const handlePathChange = () => setPath(currentPath())
    window.addEventListener('popstate', handlePathChange)
    return () => window.removeEventListener('popstate', handlePathChange)
  }, [])

  let page: ReactNode
  const dashboardMatch = path.match(/^\/groups\/(\d+)\/dashboard$/)
  const balanceMatch = path.match(/^\/groups\/(\d+)\/balances$/)
  const newExpenseMatch = path.match(/^\/groups\/(\d+)\/expenses\/new$/)
  const editExpenseMatch = path.match(/^\/groups\/(\d+)\/expenses\/(\d+)\/edit$/)
  const expenseMatch = path.match(/^\/groups\/(\d+)\/expenses\/(\d+)$/)
  const groupMatch = path.match(/^\/groups\/(\d+)$/)
  const invitationMatch = path.match(/^\/invitations\/([^/]+)$/)

  if (path === '/') {
    page = isAuthenticated ? <GroupListPage /> : <LandingPage />
  } else if (path === '/login') {
    page = isAuthenticated ? <GroupListPage /> : <LoginPage />
  } else if (path === '/register') {
    page = isAuthenticated ? <GroupListPage /> : <RegisterPage />
  } else if (path === '/groups') {
    page = isAuthenticated ? <GroupListPage /> : <RedirectToLogin returnTo={path} />
  } else if (path === '/groups/new') {
    page = isAuthenticated ? <CreateGroupPage /> : <RedirectToLogin returnTo={path} />
  } else if (path === '/account') {
    page = isAuthenticated ? <AccountPage /> : <RedirectToLogin returnTo={path} />
  } else if (dashboardMatch) {
    page = isAuthenticated
      ? <DashboardPage groupId={Number(dashboardMatch[1])} />
      : <RedirectToLogin returnTo={path} />
  } else if (balanceMatch) {
    page = isAuthenticated
      ? <BalancePage groupId={Number(balanceMatch[1])} />
      : <RedirectToLogin returnTo={path} />
  } else if (newExpenseMatch) {
    page = isAuthenticated
      ? <ExpenseFormPage groupId={Number(newExpenseMatch[1])} />
      : <RedirectToLogin returnTo={path} />
  } else if (editExpenseMatch) {
    page = isAuthenticated
      ? <ExpenseFormPage groupId={Number(editExpenseMatch[1])} expenseId={Number(editExpenseMatch[2])} />
      : <RedirectToLogin returnTo={path} />
  } else if (expenseMatch) {
    page = isAuthenticated
      ? <ExpenseDetailPage groupId={Number(expenseMatch[1])} expenseId={Number(expenseMatch[2])} />
      : <RedirectToLogin returnTo={path} />
  } else if (groupMatch) {
    page = isAuthenticated
      ? <GroupDetailPage groupId={Number(groupMatch[1])} />
      : <RedirectToLogin returnTo={path} />
  } else if (invitationMatch) {
    page = isAuthenticated
      ? <InvitationAcceptPage token={decodeURIComponent(invitationMatch[1])} />
      : <RedirectToLogin returnTo={path} />
  } else {
    page = <NotFoundPage />
  }

  return (
    <main className="app-container">
      <AppHeader />
      {page}
    </main>
  )
}
