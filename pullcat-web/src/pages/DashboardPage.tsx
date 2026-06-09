import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { StatusBadge } from '../components/StatusBadge'
import type { ReviewSessionRespDTO, StatsOverviewRespDTO } from '../types/review'
import { getReviews, getStatsOverview } from '../lib/api'

function getIssueCount(review: ReviewSessionRespDTO) {
  return Object.values(review.analyses).reduce((sum, analysis) => sum + (analysis.issues?.length || 0), 0)
}

export function DashboardPage() {
  const [stats, setStats] = useState<StatsOverviewRespDTO | null>(null)
  const [recentReviews, setRecentReviews] = useState<ReviewSessionRespDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    setLoading(true)
    setErrorMessage(null)

    Promise.all([
      getStatsOverview(),
      getReviews(0, 10),
    ]).then(([statsResponse, reviewsResponse]) => {
      if (cancelled) return
      setStats(statsResponse)
      setRecentReviews(reviewsResponse.items)
    }).catch(error => {
      if (cancelled) return
      const message = error instanceof Error ? error.message : '加载仪表盘数据失败'
      setErrorMessage(message)
      toast.error(message)
    }).finally(() => {
      if (!cancelled) setLoading(false)
    })

    return () => {
      cancelled = true
    }
  }, [])

  if (loading) {
    return (
      <div className="page-shell py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 w-40 rounded bg-slate-200 dark:bg-slate-800" />
          <div className="h-52 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          <div className="h-72 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          <div className="h-64 rounded-2xl bg-slate-200 dark:bg-slate-800" />
        </div>
      </div>
    )
  }

  const highSeverityCount = ((stats?.severityDistribution?.CRITICAL ?? 0) + (stats?.severityDistribution?.HIGH ?? 0))
  const activeReviewCount = recentReviews.filter(review => review.status === 'ANALYZING' || review.status === 'FETCHING').length
  const latestReview = recentReviews[0] ?? null

  const summaryItems = [
    { label: '进行中', value: activeReviewCount },
    { label: '仓库', value: stats?.repoCount ?? 0 },
    { label: '高严重度', value: highSeverityCount },
  ]

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card px-5 py-5 md:px-6 md:py-6">
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_18rem] lg:items-start">
          <div className="space-y-5">
            <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">仪表盘</h1>

            {latestReview ? (
              <div className="space-y-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 space-y-2">
                    <div className="truncate text-xl font-semibold tracking-tight text-slate-950 dark:text-white">
                      {latestReview.prMetadata?.title || latestReview.prUrl}
                    </div>
                    <div className="flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
                      <span>{latestReview.repositoryFullName || '-'}</span>
                      <span className="h-1 w-1 rounded-full bg-slate-300 dark:bg-slate-600" />
                      <span>{new Date(latestReview.createdAt).toLocaleDateString('zh-CN')}</span>
                      <span className="h-1 w-1 rounded-full bg-slate-300 dark:bg-slate-600" />
                      <span>{getIssueCount(latestReview)} 个问题</span>
                    </div>
                  </div>
                  <StatusBadge status={latestReview.status} />
                </div>

                <Link
                  to={`/review/${latestReview.id}`}
                  className="inline-flex min-h-11 items-center justify-center rounded-xl bg-slate-950 px-5 text-sm font-semibold text-white transition-colors hover:bg-slate-800 dark:bg-white dark:text-slate-950 dark:hover:bg-slate-200"
                >
                  打开最近审查
                </Link>
              </div>
            ) : (
              <div>
                <Link
                  to="/"
                  className="inline-flex min-h-11 items-center justify-center rounded-xl bg-emerald-700 px-5 text-sm font-semibold text-white transition-colors hover:bg-emerald-800"
                >
                  新建审查
                </Link>
              </div>
            )}
          </div>

          <dl className="grid gap-3">
            {summaryItems.map(item => (
              <div key={item.label} className="rounded-2xl bg-slate-50 px-4 py-4 dark:bg-slate-900/60">
                <dt className="text-sm font-medium text-slate-500 dark:text-slate-400">{item.label}</dt>
                <dd className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{item.value}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      {errorMessage && (
        <section className="flex flex-col gap-3 rounded-2xl border border-amber-200 bg-amber-50/80 px-5 py-4 md:flex-row md:items-center md:justify-between dark:border-amber-900/40 dark:bg-amber-950/20">
          <div className="space-y-1">
            <h2 className="text-sm font-semibold text-amber-900 dark:text-amber-200">仪表盘暂时不可用</h2>
            <p className="text-sm leading-6 text-amber-800/80 dark:text-amber-200/80">{errorMessage}</p>
          </div>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="inline-flex min-h-10 items-center justify-center rounded-xl border border-amber-300 bg-white px-4 text-sm font-semibold text-amber-900 transition-colors hover:bg-amber-50 dark:border-amber-800 dark:bg-transparent dark:text-amber-200 dark:hover:bg-amber-950/30"
          >
            重新加载
          </button>
        </section>
      )}

      <section className="surface-card overflow-hidden">
        <div className="border-b border-slate-200 px-5 py-4 dark:border-slate-800 md:px-6">
          <h2 className="text-lg font-semibold text-slate-950 dark:text-white">最近审查</h2>
        </div>
        {recentReviews.length === 0 ? (
          <div className="px-5 py-10 text-sm text-slate-500 dark:text-slate-400 md:px-6">
            还没有审查记录
          </div>
        ) : (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {recentReviews.map(review => (
              <Link
                key={review.id}
                to={`/review/${review.id}`}
                className="grid gap-4 px-5 py-4 transition-colors hover:bg-slate-50 dark:hover:bg-slate-900/60 md:grid-cols-[minmax(0,1fr)_auto] md:items-center md:px-6"
              >
                <div className="min-w-0">
                  <div className="truncate text-sm font-semibold text-slate-950 dark:text-white">
                    {review.prMetadata?.title || review.prUrl}
                  </div>
                  <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                    <span>{review.repositoryFullName || '-'}</span>
                    <span className="h-1 w-1 rounded-full bg-slate-300 dark:bg-slate-600" />
                    <span>{new Date(review.createdAt).toLocaleDateString('zh-CN')}</span>
                    <span className="h-1 w-1 rounded-full bg-slate-300 dark:bg-slate-600" />
                    <span>{getIssueCount(review)} 个问题</span>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={review.status} />
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
