import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { toast, Toaster } from 'sonner'
import * as Tooltip from '@radix-ui/react-tooltip'
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import { ThemeToggle } from './ThemeToggle'
import { getCurrentUser, logout } from '../lib/api'
import type { CurrentUserRespDTO } from '../types/review'

interface LayoutProps {
  children: ReactNode
}

const NAV_ITEMS = [
  { path: '/', label: '新建审查' },
  { path: '/history', label: '审查历史' },
  { path: '/stats', label: '统计' },
]

const GITHUB_APP_INSTALL_URL = (import.meta.env.VITE_GITHUB_APP_INSTALL_URL as string | undefined)
  ?? 'https://github.com/apps/pullkitty/installations/new'

export function Layout({ children }: LayoutProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const [user, setUser] = useState<CurrentUserRespDTO>({ authenticated: false })
  const [mobileMenuState, setMobileMenuState] = useState({ open: false, path: '/' })
  const [avatarPressed, setAvatarPressed] = useState(false)
  const [suppressTooltip, setSuppressTooltip] = useState(false)
  const [isMobileViewport, setIsMobileViewport] = useState(() => window.innerWidth < 640)

  useEffect(() => {
    let cancelled = false
    getCurrentUser().then(u => { if (!cancelled) setUser(u) }).catch(() => {})
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    const handleResize = () => {
      setIsMobileViewport(window.innerWidth < 640)
    }

    window.addEventListener('resize', handleResize)
    return () => {
      window.removeEventListener('resize', handleResize)
    }
  }, [])

  useEffect(() => {
    if (!isMobileViewport) {
      setMobileMenuState(state => (state.open ? { open: false, path: location.pathname } : state))
    }
  }, [isMobileViewport, location.pathname])

  const mobileMenuOpen = mobileMenuState.open && mobileMenuState.path === location.pathname
  const showInstallBanner = Boolean(
    user.authenticated && !user.hasInstallation && GITHUB_APP_INSTALL_URL && location.pathname.startsWith('/dashboard'),
  )

  const activePath = location.pathname === '/'
    ? '/'
    : NAV_ITEMS.find(item => item.path !== '/' && location.pathname.startsWith(item.path))?.path ?? null

  useEffect(() => {
    let cancelled = false

    async function refreshUser() {
      try {
        const nextUser = await getCurrentUser()
        if (!cancelled) {
          setUser(nextUser)
        }
      } catch {
        // ignore refresh error
      }
    }

    if (new URLSearchParams(location.search).get('installed') === 'true') {
      refreshUser().finally(() => {
        if (!cancelled) {
          navigate(location.pathname, { replace: true })
        }
      })
    }

    return () => {
      cancelled = true
    }
  }, [location.pathname, location.search, navigate])

  return (
    <Tooltip.Provider delayDuration={500}>
      <div className="min-h-screen font-sans transition-colors duration-300">
        <header className="app-header">
          <div className="app-header__brand">
            <Link to="/" className="app-brand">
              <img src="/cat.png" alt="Pullcat" className="h-7 w-auto object-contain md:h-9" />
              <span className="app-brand__wordmark">Pullcat</span>
            </Link>
            <nav className="app-nav app-nav--desktop" aria-label="主导航">
              {NAV_ITEMS.map(item => (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`app-nav__link ${activePath === item.path ? 'app-nav__link--active' : ''}`}
                >
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>

          <div className="app-header__actions">
            <a
              href="https://github.com/xiechimon/pullcat"
              target="_blank"
              rel="noopener noreferrer"
              aria-label="View on GitHub"
              className="app-header__icon-button"
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
              </svg>
            </a>
            <ThemeToggle />
            {user.authenticated ? (
              <DropdownMenu.Root onOpenChange={open => { setAvatarPressed(open); if (!open) { setSuppressTooltip(true); setTimeout(() => setSuppressTooltip(false), 300); } }}>
                <Tooltip.Root open={(avatarPressed || suppressTooltip) ? false : undefined}>
                  <Tooltip.Trigger asChild>
                    <DropdownMenu.Trigger asChild>
                      <button style={{ transition: 'box-shadow 0.1s', boxShadow: avatarPressed ? '0px 0px 0px 4px rgba(0,0,0,0.15)' : '' }} className="flex items-center gap-2 rounded-full focus:outline-none">
                        {user.avatarUrl ? (
                          <img src={user.avatarUrl} alt="" className="w-8 h-8 rounded-full" />
                        ) : (
                          <span className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center text-xs font-bold">
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
                          try {
                            await logout()
                            setUser({ authenticated: false })
                            toast.success('已退出登录')
                            navigate('/login')
                          } catch {
                            toast.error('退出失败')
                          }
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
              <Link to="/login" className="app-login-link">
                登录
              </Link>
            )}
            {isMobileViewport && (
              <button
                type="button"
                className="app-nav-trigger sm:hidden"
                aria-label="打开导航菜单"
                aria-expanded={mobileMenuOpen}
                onClick={() => setMobileMenuState(state => ({
                  open: state.path === location.pathname ? !state.open : true,
                  path: location.pathname,
                }))}
              >
                <span className="app-nav-trigger__line" />
                <span className="app-nav-trigger__line" />
                <span className="app-nav-trigger__line" />
              </button>
            )}
          </div>
        </header>

        {isMobileViewport && mobileMenuOpen && (
          <div className="app-mobile-nav sm:hidden" role="dialog" aria-label="移动端导航">
            <nav className="app-mobile-nav__list" aria-label="移动端导航链接">
              {NAV_ITEMS.map(item => (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`app-mobile-nav__link ${activePath === item.path ? 'app-mobile-nav__link--active' : ''}`}
                  onClick={() => setMobileMenuState({ open: false, path: item.path })}
                >
                  {item.label}
                </Link>
              ))}
              {user.authenticated ? (
                <Link
                  to="/settings"
                  className="app-mobile-nav__link"
                  onClick={() => setMobileMenuState({ open: false, path: '/settings' })}
                >
                  设置
                </Link>
              ) : (
                <Link
                  to="/login"
                  className="app-mobile-nav__link"
                  onClick={() => setMobileMenuState({ open: false, path: '/login' })}
                >
                  登录
                </Link>
              )}
            </nav>
          </div>
        )}

        <main className="pt-[88px] pb-20 md:pt-[96px]">
          {showInstallBanner && (
            <div className="mx-auto mb-6 flex w-full max-w-6xl items-center justify-between gap-4 rounded-2xl border border-amber-300 bg-amber-50 px-5 py-4 text-sm text-amber-950 shadow-sm dark:border-amber-700/60 dark:bg-amber-950/40 dark:text-amber-100">
              <div className="flex flex-col gap-1">
                <span className="font-semibold">未安装 GitHub App</span>
                <span>安装后 PR 创建时将自动触发分析</span>
              </div>
              <a
                href={GITHUB_APP_INSTALL_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="shrink-0 rounded-full bg-amber-900 px-4 py-2 text-xs font-semibold text-white transition hover:bg-amber-800 dark:bg-amber-300 dark:text-amber-950 dark:hover:bg-amber-200"
              >
                立即安装
              </a>
            </div>
          )}
          {children}
        </main>
        <Toaster richColors position="top-center" />
      </div>
    </Tooltip.Provider>
  )
}
