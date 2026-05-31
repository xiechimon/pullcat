import { useEffect, useState, useRef } from 'react'
import { toast } from 'sonner'
import type { Repo } from '../types/review'
import { getRepos, addRepo, deleteRepo } from '../lib/api'

interface RepoSelectorProps {
  value?: string
  onChange: (fullName: string) => void
}

export function RepoSelector({ value, onChange }: RepoSelectorProps) {
  const [repos, setRepos] = useState<Repo[]>([])
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let cancelled = false
    getRepos().then(data => { if (!cancelled) setRepos(data) }).catch(() => {})
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => {
      cancelled = true
      document.removeEventListener('mousedown', handler)
    }
  }, [])

  const filtered = repos.filter(r =>
    r.fullName.toLowerCase().includes(search.toLowerCase())
  )

  const handleAdd = async () => {
    const parts = search.split('/')
    if (parts.length !== 2) return
    try {
      const r = await addRepo(parts[0], parts[1])
      setRepos(prev => [...prev, r])
      setSearch('')
      toast.success(`已收藏 ${r.fullName}`)
    } catch (e) {
      toast.error((e as Error).message)
    }
  }

  const handleDelete = async (owner: string, repo: string) => {
    try {
      await deleteRepo(owner, repo)
      setRepos(prev => prev.filter(r => r.owner !== owner || r.repo !== repo))
      toast.success(`已移除 ${owner}/${repo}`)
    } catch (e) {
      toast.error((e as Error).message)
    }
  }

  return (
    <div ref={ref} className="relative">
      <input
        type="text"
        value={value || search}
        placeholder="搜索仓库 (owner/repo)..."
        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-slate-800 text-sm"
        onFocus={() => setOpen(true)}
        onChange={e => {
          setSearch(e.target.value)
          onChange('')
        }}
        onKeyDown={e => {
          if (e.key === 'Enter') {
            handleAdd()
          }
        }}
      />
      {open && (
        <div className="absolute top-full mt-1 w-full bg-white dark:bg-slate-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg z-50 max-h-60 overflow-y-auto">
          {filtered.length === 0 && search.includes('/') && (
            <button
              onClick={handleAdd}
              className="w-full px-4 py-2 text-left text-sm text-emerald-600 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              收藏仓库: {search}
            </button>
          )}
          {filtered.map(r => (
            <div
              key={r.fullName}
              className="flex items-center justify-between px-4 py-2 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer"
              onClick={() => {
                onChange(r.fullName)
                setOpen(false)
              }}
            >
              <span className="text-sm text-gray-900 dark:text-white">{r.fullName}</span>
              <button
                onClick={e => {
                  e.stopPropagation()
                  handleDelete(r.owner, r.repo)
                }}
                className="text-xs text-red-400 hover:text-red-600"
              >
                移除
              </button>
            </div>
          ))}
          {search && !search.includes('/') && filtered.length === 0 && (
            <div className="px-4 py-2 text-xs text-gray-500">输入 owner/repo 格式来收藏仓库</div>
          )}
        </div>
      )}
    </div>
  )
}
