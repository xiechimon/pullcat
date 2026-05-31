import type { ReactNode } from 'react'
import { useEffect, useState, useRef } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ThemeToggle } from './ThemeToggle'
import { getCurrentUser } from '../lib/api'

interface LayoutProps {
  children: ReactNode
}

const NAV_ITEMS = [
  { path: '/dashboard', label: '仪表盘' },
  { path: '/history', label: '历史' },
  { path: '/stats', label: '统计' },
  { path: '/', label: '新建审查' },
]

export function Layout({ children }: LayoutProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const [user, setUser] = useState<{ authenticated: boolean; login?: string; avatarUrl?: string }>({ authenticated: false })
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let cancelled = false
    getCurrentUser().then(u => { if (!cancelled) setUser(u) }).catch(() => {})
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  return (
    <div className="min-h-screen font-sans transition-colors duration-300">
      <header className="fixed h-[60px] md:h-[80px] top-0 left-0 right-0 z-50 bg-white/80 dark:bg-slate-900/80 backdrop-blur-sm border-b border-emerald-700/30 flex flex-row items-center justify-between px-4 lg:px-16">
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-3">
            <img src="/cat.png" alt="Pullcat" className="h-7 md:h-10 w-auto object-contain" />
            <span className="font-serif font-bold text-2xl tracking-tighter text-emerald-700">Pullcat</span>
          </Link>
          <nav className="hidden sm:flex items-center gap-1">
            {NAV_ITEMS.map(item => (
              <Link
                key={item.path}
                to={item.path}
                className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  location.pathname === item.path
                    ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-400'
                    : 'text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800'
                }`}
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>
        <div className="flex items-center gap-3">
          <ThemeToggle />
          <div ref={menuRef} className="relative">
            {user.authenticated ? (
              <>
                <button
                  onClick={() => setMenuOpen(!menuOpen)}
                  className="flex items-center gap-2"
                >
                  {user.avatarUrl ? (
                    <img src={user.avatarUrl} alt="" className="w-7 h-7 rounded-full ring-2 ring-emerald-200" />
                  ) : (
                    <span className="w-7 h-7 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center text-xs font-bold">
                      {user.login?.[0]?.toUpperCase()}
                    </span>
                  )}
                </button>
                {menuOpen && (
                  <div className="absolute right-0 top-full mt-1 w-48 bg-white dark:bg-slate-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg py-1 z-50">
                    <div className="px-4 py-2 text-sm text-gray-500 border-b border-gray-100 dark:border-gray-700">
                      {user.login}
                    </div>
                    <Link to="/settings" onClick={() => setMenuOpen(false)}
                      className="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700">
                      设置
                    </Link>
                    <a
                      href="/logout"
                      onClick={async (e) => {
                        e.preventDefault()
                        await fetch('/api/logout', { method: 'POST' })
                        setUser({ authenticated: false })
                        setMenuOpen(false)
                        navigate('/login')
                      }}
                      className="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                    >
                      退出
                    </a>
                  </div>
                )}
              </>
            ) : (
              <Link to="/login" className="text-sm font-medium text-emerald-700 hover:underline">
                登录
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="pt-[80px] pb-20">
        {children}
      </main>
    </div>
  )
}
