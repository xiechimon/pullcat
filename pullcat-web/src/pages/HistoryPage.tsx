import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { StatusBadge } from '../components/StatusBadge'
import type { ReviewSessionRespDTO } from '../types/review'
import { deleteReview, getReviews } from '../lib/api'

function getIssueCount(review: ReviewSessionRespDTO) {
  return Object.values(review.analyses).reduce((sum, analysis) => sum + (analysis.issues?.length || 0), 0)
}

export function HistoryPage() {
  const navigate = useNavigate()
  const [reviews, setReviews] = useState<ReviewSessionRespDTO[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)
  const [compareMode, setCompareMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [repoInput, setRepoInput] = useState('')
  const [searchParams, setSearchParams] = useSearchParams()

  const page = Number(searchParams.get('page') || '0')
  const repo = searchParams.get('repo') || ''
  const size = 20

  useEffect(() => {
    setRepoInput(repo)
  }, [repo])

  useEffect(() => {
    let cancelled = false
    setLoading(true)

    getReviews(page, size, repo || undefined)
      .then(response => {
        if (cancelled) return
        setReviews(response.items)
        setTotal(response.total)
        setLoading(false)
      })
      .catch(error => {
        if (cancelled) return
        toast.error(error instanceof Error ? error.message : '加载历史记录失败')
        setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [page, repo])

  const totalPages = Math.max(1, Math.ceil(total / size))

  const applyRepoFilter = () => {
    const value = repoInput.trim()
    setSearchParams(value ? { repo: value, page: '0' } : { page: '0' })
  }

  const clearRepoFilter = () => {
    setRepoInput('')
    setSearchParams({ page: '0' })
  }

  const handleDelete = async (id: string) => {
    try {
      await deleteReview(id)
      setReviews(prev => prev.filter(review => review.id !== id))
      setTotal(prev => Math.max(0, prev - 1))
      setSelectedIds(prev => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
      toast.success('已删除')
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '删除失败')
    }
  }

  const toggleCompareMode = () => {
    if (compareMode) {
      setCompareMode(false)
      setSelectedIds(new Set())
      return
    }

    setCompareMode(true)
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
      <div className="page-shell py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 w-40 rounded bg-slate-200 dark:bg-slate-800" />
          <div className="h-24 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          <div className="space-y-3">
            {[...Array(5)].map((_, index) => (
              <div key={index} className="h-24 rounded-2xl bg-slate-200 dark:bg-slate-800" />
            ))}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card overflow-hidden">
        <div className="grid gap-3 px-5 py-4 md:grid-cols-[minmax(0,1fr)_auto] md:px-6">
          <div className="flex min-w-0 items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50/80 p-2 dark:border-slate-800 dark:bg-slate-900/50">
            <input
              type="text"
              value={repoInput}
              onChange={(e) => setRepoInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') applyRepoFilter()
              }}
              placeholder="筛选仓库，例如 owner/repo"
              className="min-h-10 min-w-0 flex-1 rounded-xl bg-white px-4 text-sm text-slate-900 outline-none transition-colors placeholder:text-slate-400 focus:ring-2 focus:ring-emerald-500/20 dark:bg-slate-950/70 dark:text-white"
            />
            {repo && (
              <button
                type="button"
                onClick={clearRepoFilter}
                className="inline-flex min-h-10 shrink-0 items-center justify-center rounded-xl px-3 text-sm font-medium text-slate-500 transition-colors hover:bg-white hover:text-slate-700 dark:text-slate-300 dark:hover:bg-slate-950/70 dark:hover:text-white"
              >
                清除
              </button>
            )}
            <button
              type="button"
              onClick={applyRepoFilter}
              className="inline-flex min-h-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-950/70 dark:text-slate-200 dark:hover:bg-slate-900/80"
            >
              应用
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-2 md:justify-end">
            {compareMode && (
              <span className="inline-flex min-h-10 items-center rounded-xl bg-slate-100 px-3 text-sm font-medium text-slate-500 dark:bg-slate-900/70 dark:text-slate-300">
                已选 {selectedIds.size}/2
              </span>
            )}
            {compareMode && (
              <button
                type="button"
                disabled={selectedIds.size !== 2}
                onClick={startCompare}
                className="inline-flex min-h-10 items-center justify-center rounded-xl bg-slate-950 px-4 text-sm font-semibold text-white transition-colors hover:bg-slate-800 disabled:pointer-events-none disabled:opacity-35 dark:bg-white dark:text-slate-950 dark:hover:bg-slate-200"
              >
                开始对比
              </button>
            )}
            <button
              type="button"
              onClick={toggleCompareMode}
              className={`inline-flex min-h-10 items-center justify-center rounded-xl px-4 text-sm font-semibold transition-colors ${
                compareMode
                  ? 'border border-slate-200 text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-900/70'
                  : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 dark:bg-emerald-950/30 dark:text-emerald-300 dark:hover:bg-emerald-950/50'
              }`}
            >
              {compareMode ? '取消对比' : '选择对比'}
            </button>
          </div>
        </div>
      </section>

      {reviews.length === 0 ? (
        <section className="surface-card px-5 py-10 text-center md:px-6">
          <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-6 py-8 dark:border-slate-700 dark:bg-slate-900/40">
            <h2 className="text-lg font-semibold text-slate-950 dark:text-white">暂无审查记录</h2>
            <Link
              to="/"
              className="mt-5 inline-flex min-h-11 items-center justify-center rounded-xl bg-emerald-700 px-5 text-sm font-semibold text-white transition-colors hover:bg-emerald-800"
            >
              开始审查
            </Link>
          </div>
        </section>
      ) : (
        <>
          <section className="surface-card overflow-hidden">
            <div className="divide-y divide-slate-100 dark:divide-slate-800">
              {reviews.map(review => {
                const issueCount = getIssueCount(review)
                const isSelected = selectedIds.has(review.id)
                const disableUnchecked = !isSelected && selectedIds.size >= 2

                return (
                  <div
                    key={review.id}
                    className={`grid gap-4 px-5 py-4 transition-colors hover:bg-slate-50 dark:hover:bg-slate-900/60 md:grid-cols-[auto_minmax(0,1fr)_auto] md:items-center md:px-6 ${
                      isSelected ? 'bg-emerald-50/70 dark:bg-emerald-950/20' : ''
                    }`}
                  >
                    {compareMode ? (
                      <label className="flex items-center justify-center pt-1 md:pt-0">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          disabled={disableUnchecked}
                          onChange={() => toggleSelect(review.id)}
                          className="h-4 w-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-400"
                        />
                      </label>
                    ) : (
                      <div className="hidden md:block" />
                    )}

                    <div className="min-w-0 space-y-3">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <Link
                          to={`/review/${review.id}`}
                          className="min-w-0 flex-1 truncate text-base font-semibold text-slate-950 transition-colors hover:text-emerald-700 dark:text-white dark:hover:text-emerald-300"
                        >
                          {review.prMetadata?.title || review.prUrl}
                        </Link>
                      </div>

                      <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                        <span>{review.repositoryFullName || '-'}</span>
                        <span className="h-1 w-1 rounded-full bg-slate-300 dark:bg-slate-600" />
                        <span>{new Date(review.createdAt).toLocaleDateString('zh-CN')}</span>
                        <span className="h-1 w-1 rounded-full bg-slate-300 dark:bg-slate-600" />
                        <span>{issueCount} 个问题</span>
                      </div>
                    </div>

                    <div className="flex items-center gap-3 md:min-w-28 md:justify-end">
                      <div className="flex items-center">
                        <StatusBadge status={review.status} />
                      </div>
                      <button
                        type="button"
                        onClick={() => setDeleteTarget(review.id)}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-red-600 dark:text-slate-500 dark:hover:bg-slate-900/70 dark:hover:text-red-400"
                        aria-label="删除审查记录"
                      >
                        <svg viewBox="0 0 20 20" fill="none" className="h-4 w-4" aria-hidden="true">
                          <path d="M6.5 7.5V14.25M10 7.5V14.25M13.5 7.5V14.25M3.75 5.25H16.25M12.75 5.25V4.5C12.75 3.81 12.19 3.25 11.5 3.25H8.5C7.81 3.25 7.25 3.81 7.25 4.5V5.25M5.5 5.25H14.5V15.25C14.5 15.94 13.94 16.5 13.25 16.5H6.75C6.06 16.5 5.5 15.94 5.5 15.25V5.25Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        </svg>
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          </section>

          <section className="flex items-center justify-center gap-4">
            <button
              type="button"
              disabled={page <= 0}
              onClick={() => setSearchParams({ page: String(page - 1), ...(repo ? { repo } : {}) })}
              className="inline-flex min-h-10 items-center justify-center rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-50 disabled:pointer-events-none disabled:opacity-35 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-900/70"
            >
              上一页
            </button>
            <span className="text-sm text-slate-500 dark:text-slate-400">
              {page + 1} / {totalPages}
            </span>
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setSearchParams({ page: String(page + 1), ...(repo ? { repo } : {}) })}
              className="inline-flex min-h-10 items-center justify-center rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-50 disabled:pointer-events-none disabled:opacity-35 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-900/70"
            >
              下一页
            </button>
          </section>
        </>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null)
        }}
        title="确认删除"
        description="确定删除此审查记录？此操作不可撤销"
        confirmLabel="删除"
        onConfirm={() => {
          if (deleteTarget) handleDelete(deleteTarget)
        }}
      />
    </div>
  )
}
