import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Toaster } from 'sonner'
import * as Tooltip from '@radix-ui/react-tooltip'
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
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

  useEffect(() => {
    let cancelled = false
    getCurrentUser().then(u => { if (!cancelled) setUser(u) }).catch(() => {})
    return () => { cancelled = true }
  }, [])

  return (
    <Tooltip.Provider delayDuration={500}>
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
          {user.authenticated ? (
            <DropdownMenu.Root>
              <Tooltip.Root>
                <Tooltip.Trigger asChild>
                  <DropdownMenu.Trigger asChild>
                    <button className="flex items-center gap-2">
                      {user.avatarUrl ? (
                        <img src={user.avatarUrl} alt="" className="w-7 h-7 rounded-full ring-2 ring-emerald-200" />
                      ) : (
                        <span className="w-7 h-7 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center text-xs font-bold">
                          {user.login?.[0]?.toUpperCase()}
                        </span>
                      )}
                    </button>
                  </DropdownMenu.Trigger>
                </Tooltip.Trigger>
                <Tooltip.Portal>
                  <Tooltip.Content
                    className="px-2 py-1 text-xs text-white bg-gray-900 dark:bg-gray-100 dark:text-gray-900 rounded shadow-lg z-50"
                    sideOffset={5}
                  >
                    {user.login}
                    <Tooltip.Arrow className="fill-gray-900 dark:fill-gray-100" />
                  </Tooltip.Content>
                </Tooltip.Portal>
              </Tooltip.Root>
              <DropdownMenu.Portal>
                <DropdownMenu.Content
                  className="min-w-48 bg-white dark:bg-slate-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg py-1 z-50"
                  sideOffset={5}
                  align="end"
                >
                  <div className="px-4 py-2 text-sm text-gray-500 border-b border-gray-100 dark:border-gray-700">
                    {user.login}
                  </div>
                  <DropdownMenu.Item asChild>
                    <Link to="/settings" className="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer outline-none">
                      设置
                    </Link>
                  </DropdownMenu.Item>
                  <DropdownMenu.Item asChild>
                    <a
                      href="/logout"
                      onClick={async (e) => {
                        e.preventDefault()
                        await fetch('/api/logout', { method: 'POST', credentials: 'include' })
                        setUser({ authenticated: false })
                        navigate('/login')
                      }}
                      className="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer outline-none"
                    >
                      退出
                    </a>
                  </DropdownMenu.Item>
                </DropdownMenu.Content>
              </DropdownMenu.Portal>
            </DropdownMenu.Root>
          ) : (
            <Link to="/login" className="text-sm font-medium text-emerald-700 hover:underline">
              登录
            </Link>
          )}
        </div>
      </header>

      <main className="pt-[80px] pb-20">
        {children}
      </main>
      <Toaster richColors position="top-center" />
    </div>
    </Tooltip.Provider>
  )
}
