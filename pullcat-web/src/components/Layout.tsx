import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ThemeToggle } from './ThemeToggle'

interface LayoutProps {
  children: ReactNode
}

export function Layout({ children }: LayoutProps) {
  return (
    <div className="min-h-screen font-sans transition-colors duration-300">
      <header className="fixed h-[60px] md:h-[80px] top-0 left-0 right-0 z-50 bg-white/80 dark:bg-slate-900/80 backdrop-blur-sm border-b border-emerald-700/30 flex flex-row items-center justify-between px-4 lg:px-16">
        <Link to="/" className="flex items-center gap-3">
          <img src="/cat.png" alt="Pullcat" className="h-7 md:h-10 w-auto object-contain" />
          <span className="font-serif font-bold text-2xl tracking-tighter text-emerald-700">Pullcat</span>
        </Link>
        <div className="flex items-center gap-2">
          <a href="#" className="hidden sm:block text-sm font-semibold text-emerald-700 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors px-3 py-2 rounded-lg">Docs</a>
          <a href="#" className="text-sm font-semibold text-emerald-700 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors px-3 py-2 rounded-lg">Console</a>
          <ThemeToggle />
        </div>
      </header>

      <main className="pt-[80px] pb-20">
        {children}
      </main>
    </div>
  )
}
