import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { StatusBadge } from '../components/StatusBadge'
import type { ReviewSessionRespDTO, Severity, StatsOverviewRespDTO } from '../types/review'
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
          <div className="grid gap-4 md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
            <div className="h-56 rounded-2xl bg-slate-200 dark:bg-slate-800" />
            <div className="h-56 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          </div>
        </div>
      </div>
    )
  }

  const totalReviews = stats?.totalReviews ?? 0
  const totalIssues = stats?.totalIssues ?? 0
  const avgIssues = (stats?.avgIssuesPerReview ?? 0).toFixed(1)
  const highSeverityCount = ((stats?.severityDistribution?.CRITICAL ?? 0) + (stats?.severityDistribution?.HIGH ?? 0))
  const activeReviewCount = recentReviews.filter(review => review.status === 'ANALYZING' || review.status === 'FETCHING').length
  const latestReview = recentReviews[0] ?? null

  const summaryItems = [
    { label: '活跃审查', value: activeReviewCount, hint: activeReviewCount > 0 ? '仍在分析中' : '当前没有进行中的会话' },
    { label: '覆盖仓库', value: stats?.repoCount ?? 0, hint: '最近统计范围' },
  ]

  const severitySummary: Array<{ label: string; count: number; tone: string }> = [
    { label: '高严重度', count: highSeverityCount, tone: 'bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-300' },
    { label: '中严重度', count: stats?.severityDistribution?.MEDIUM ?? 0, tone: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-950/40 dark:text-yellow-300' },
    { label: '低严重度', count: (stats?.severityDistribution?.LOW ?? 0) + (stats?.severityDistribution?.INFO ?? 0), tone: 'bg-slate-100 text-slate-700 dark:bg-slate-900 dark:text-slate-300' },
  ]

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card overflow-hidden">
        {latestReview ? (
          <div className="grid gap-0 md:grid-cols-[minmax(0,1.28fr)_14.5rem]">
            <div className="px-5 py-5 md:px-6 md:py-6">
              <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                  <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
                  <div className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">
                    {latestReview.prMetadata?.title || latestReview.prUrl}
                  </div>
                </div>
                <StatusBadge status={latestReview.status} />
              </div>

              <div className="mt-4 flex flex-wrap items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
                <span>{latestReview.repositoryFullName || '-'}</span>
                <span className="h-1 w-1 rounded-full bg-slate-300 dark:bg-slate-600" />
                <span>{new Date(latestReview.createdAt).toLocaleDateString('zh-CN')}</span>
                <span className="h-1 w-1 rounded-full bg-slate-300 dark:bg-slate-600" />
                <span>{getIssueCount(latestReview)} 个问题</span>
              </div>

              <div className="mt-5 flex flex-wrap gap-2 text-xs text-slate-500 dark:text-slate-400">
                <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1.5 dark:bg-slate-900/70">查看问题</span>
                <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1.5 dark:bg-slate-900/70">确认反馈</span>
                <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1.5 dark:bg-slate-900/70">继续处理</span>
              </div>

              <Link
                to={`/review/${latestReview.id}`}
                className="mt-6 inline-flex min-h-11 items-center justify-center rounded-xl bg-slate-950 px-5 text-sm font-semibold text-white transition-colors hover:bg-slate-800 dark:bg-white dark:text-slate-950 dark:hover:bg-slate-200"
              >
                打开最近审查
              </Link>
            </div>

            <div className="border-t border-slate-200 bg-slate-50/70 px-5 py-5 dark:border-slate-800 dark:bg-slate-900/50 md:border-l md:border-t-0 md:px-6">
              <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">工作摘要</div>
              <dl className="mt-4 space-y-3">
                {summaryItems.map(item => (
                  <div key={item.label} className="border-b border-slate-200/80 pb-3 last:border-b-0 last:pb-0 dark:border-slate-800">
                    <div className="flex items-end justify-between gap-3">
                      <dt className="text-sm font-medium text-slate-500 dark:text-slate-400">{item.label}</dt>
                      <dd className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{item.value}</dd>
                    </div>
                    <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">{item.hint}</div>
                  </div>
                ))}
              </dl>

              <div className="mt-4 border-t border-slate-200 pt-4 dark:border-slate-800">
                <div className="text-xs font-medium text-slate-400 dark:text-slate-500">严重度分布</div>
                <div className="mt-3 flex flex-wrap gap-2">
                {severitySummary.map(item => (
                  <span key={item.label} className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-medium ${item.tone}`}>
                    {item.label}
                    <span>{item.count}</span>
                  </span>
                ))}
                </div>
              </div>
            </div>
          </div>
        ) : (
          <div className="px-5 py-5 md:px-6 md:py-6">
            <div className="grid gap-6 md:grid-cols-[minmax(0,1fr)_16rem] md:items-start">
              <div className="space-y-4">
                <div className="space-y-1">
                  <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
                  <h2 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">开始第一条审查</h2>
                </div>

                <div className="flex flex-wrap gap-2 text-xs text-slate-500 dark:text-slate-400">
                  <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1.5 dark:bg-slate-900/70">公开仓库 PR</span>
                  <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1.5 dark:bg-slate-900/70">自动建会话</span>
                  <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1.5 dark:bg-slate-900/70">实时审查</span>
                </div>

                <Link
                  to="/"
                  className="inline-flex min-h-11 items-center justify-center rounded-xl bg-emerald-700 px-5 text-sm font-semibold text-white transition-colors hover:bg-emerald-800"
                >
                  新建审查
                </Link>
              </div>

              <div className="grid gap-3 rounded-2xl border border-slate-200 bg-slate-50/80 p-4 dark:border-slate-800 dark:bg-slate-900/50">
                <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">工作摘要</div>
                <div className="grid gap-3">
                  {summaryItems.map(item => (
                    <div key={item.label} className="flex items-center justify-between gap-3 rounded-xl bg-white px-3 py-3 dark:bg-slate-950/70">
                      <div className="space-y-1">
                        <div className="text-sm font-medium text-slate-500 dark:text-slate-400">{item.label}</div>
                        <div className="text-xs text-slate-400 dark:text-slate-500">{item.hint}</div>
                      </div>
                      <div className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{item.value}</div>
                    </div>
                  ))}
                </div>

                <div className="flex flex-wrap gap-2 pt-1">
                  {severitySummary.map(item => (
                    <span key={item.label} className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-medium ${item.tone}`}>
                      {item.label}
                      <span>{item.count}</span>
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
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
          <div className="px-5 py-10 md:px-6">
            <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-6 py-8 text-center dark:border-slate-700 dark:bg-slate-900/40">
              <h3 className="text-lg font-semibold text-slate-950 dark:text-white">还没有审查记录</h3>
              <Link
                to="/"
                className="mt-5 inline-flex min-h-11 items-center justify-center rounded-xl bg-emerald-700 px-5 text-sm font-semibold text-white transition-colors hover:bg-emerald-800"
              >
                开始第一次审查
              </Link>
            </div>
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
