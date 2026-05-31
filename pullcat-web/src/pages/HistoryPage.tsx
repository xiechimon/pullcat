import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { StatusBadge } from '../components/StatusBadge'
import type { ReviewSession } from '../types/review'
import { getReviews, deleteReview } from '../lib/api'

export function HistoryPage() {
  const navigate = useNavigate()
  const [reviews, setReviews] = useState<ReviewSession[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)
  const [compareMode, setCompareMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [searchParams, setSearchParams] = useSearchParams()

  const page = Number(searchParams.get('page') || '0')
  const repo = searchParams.get('repo') || undefined
  const size = 20

  useEffect(() => {
    let cancelled = false
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)
     
    getReviews(page, size, repo)
      .then(r => {
        if (!cancelled) { setReviews(r.items); setTotal(r.total); setLoading(false) }
      })
      .catch(e => { if (!cancelled) { toast.error(e.message); setLoading(false) } })
    return () => { cancelled = true }
  }, [page, repo])

  const totalPages = Math.max(1, Math.ceil(total / size))

  const handleDelete = async (id: string) => {
    try {
      await deleteReview(id)
      setReviews(prev => prev.filter(r => r.id !== id))
      toast.success('已删除')
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const toggleCompareMode = () => {
    if (compareMode) {
      setCompareMode(false)
      setSelectedIds(new Set())
    } else {
      setCompareMode(true)
    }
  }

  const toggleSelect = (id: string) => {
    setSelectedIds(prev => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else if (next.size < 2) {
        next.add(id)
      }
      return next
    })
  }

  const startCompare = () => {
    const ids = Array.from(selectedIds)
    if (ids.length === 2) {
      navigate(`/compare?r1=${ids[0]}&r2=${ids[1]}`)
    }
  }

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8">
        <div className="animate-pulse space-y-3">
          {[...Array(8)].map((_, i) => (
            <div key={i} className="h-14 bg-gray-200 dark:bg-gray-700 rounded-lg" />
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">审查历史</h1>
        <div className="flex items-center gap-2">
          {compareMode && (
            <>
              <span className="text-sm text-gray-500 dark:text-gray-400">
                已选 {selectedIds.size}/2
              </span>
              <button
                disabled={selectedIds.size !== 2}
                onClick={startCompare}
                className="px-3 py-1.5 text-sm rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-40 disabled:pointer-events-none transition-colors"
              >
                开始对比
              </button>
            </>
          )}
          <button
            onClick={toggleCompareMode}
            className={`px-3 py-1.5 text-sm rounded-lg border transition-colors ${
              compareMode
                ? 'border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                : 'border-emerald-300 dark:border-emerald-700 text-emerald-700 dark:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-900/20'
            }`}
          >
            {compareMode ? '取消' : '对比'}
          </button>
        </div>
      </div>

      <div className="flex gap-3">
        <input
          type="text"
          placeholder="按仓库筛选 (owner/repo)..."
          className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-slate-800 text-sm"
          defaultValue={repo || ''}
          onKeyDown={e => {
            if (e.key === 'Enter') {
              const val = (e.target as HTMLInputElement).value.trim()
              setSearchParams(val ? { repo: val, page: '0' } : { page: '0' })
            }
          }}
        />
        {repo && (
          <button
            onClick={() => setSearchParams({ page: '0' })}
            className="px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900"
          >
            清除
          </button>
        )}
      </div>

      {reviews.length === 0 ? (
        <div className="p-12 text-center text-gray-500 dark:text-gray-400 border border-dashed border-gray-300 dark:border-gray-600 rounded-xl">
          暂无审查记录
        </div>
      ) : (
        <>
          <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 dark:bg-gray-750">
                <tr>
                  {compareMode && <th className="w-10 px-2 py-3" />}
                  <th className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-400">PR 标题</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-400">仓库</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-400">时间</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-400">状态</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600 dark:text-gray-400">问题数</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-700">
                {reviews.map(r => {
                  const issueCount = Object.values(r.analyses).reduce((sum, a) => sum + (a.issues?.length || 0), 0)
                  return (
                    <tr key={r.id} className="hover:bg-gray-50 dark:hover:bg-gray-750">
                      {compareMode && (
                        <td className="px-2 py-3">
                          <input
                            type="checkbox"
                            checked={selectedIds.has(r.id)}
                            disabled={!selectedIds.has(r.id) && selectedIds.size >= 2}
                            onChange={() => toggleSelect(r.id)}
                            className="h-4 w-4 rounded border-gray-300 text-emerald-500 focus:ring-emerald-400"
                          />
                        </td>
                      )}
                      <td className="px-4 py-3">
                        <Link to={`/review/${r.id}`} className="text-emerald-600 hover:underline font-medium truncate block max-w-xs">
                          {r.prMetadata?.title || r.prUrl}
                        </Link>
                      </td>
                      <td className="px-4 py-3">
                        {r.repositoryFullName ? (
                          <Link to={`/repos/${r.prMetadata?.owner}/${r.prMetadata?.repo}`} className="text-gray-600 dark:text-gray-400 hover:underline">
                            {r.repositoryFullName}
                          </Link>
                        ) : '-'}
                      </td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400 whitespace-nowrap">
                        {new Date(r.createdAt).toLocaleDateString('zh-CN')}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={r.status} />
                      </td>
                      <td className="px-4 py-3 text-gray-500">{issueCount}</td>
                       <td className="px-4 py-3">
                        <button
                          onClick={() => setDeleteTarget(r.id)}
                          className="text-xs text-red-500 hover:text-red-700"
                        >
                          删除
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <div className="flex items-center justify-center gap-4">
            <button
              disabled={page <= 0}
              onClick={() => setSearchParams({ page: String(page - 1), ...(repo ? { repo } : {}) })}
              className="px-3 py-1 text-sm rounded border border-gray-300 dark:border-gray-600 disabled:opacity-30"
            >
              上一页
            </button>
            <span className="text-sm text-gray-600 dark:text-gray-400">
              {page + 1} / {totalPages}
            </span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setSearchParams({ page: String(page + 1), ...(repo ? { repo } : {}) })}
              className="px-3 py-1 text-sm rounded border border-gray-300 dark:border-gray-600 disabled:opacity-30"
            >
              下一页
            </button>
          </div>
        </>
      )}
      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => { if (!open) setDeleteTarget(null) }}
        title="确认删除"
        description="确定删除此审查记录？此操作不可撤销。"
        confirmLabel="删除"
        onConfirm={() => { if (deleteTarget) handleDelete(deleteTarget) }}
      />
    </div>
  )
}
