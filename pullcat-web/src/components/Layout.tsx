import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { ThemeToggle } from './ThemeToggle'

interface LayoutProps {
  children: ReactNode
}

const NAV_ITEMS = [
  { path: '/dashboard', label: '仪表盘' },
  { path: '/history', label: '历史' },
  { path: '/', label: '新建审查' },
]

export function Layout({ children }: LayoutProps) {
  const location = useLocation()

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
        <ThemeToggle />
      </header>

      <main className="pt-[80px] pb-20">
        {children}
      </main>
    </div>
  )
}
