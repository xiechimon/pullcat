import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { StatusBadge } from '../components/StatusBadge'
import type { RepoStatsRespDTO, ReviewSessionRespDTO, Severity } from '../types/review'
import { SEVERITY_BAR_COLORS } from '../types/review'
import { getRepoStats, getReviews } from '../lib/api'

export function RepoPage() {
  const { owner, repo } = useParams<{ owner: string; repo: string }>()
  const [reviews, setReviews] = useState<ReviewSessionRespDTO[]>([])
  const [stats, setStats] = useState<RepoStatsRespDTO | null>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()
  const fullName = `${owner}/${repo}`

  useEffect(() => {
    if (!owner || !repo) return
    let cancelled = false
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)
    Promise.all([
      getReviews(0, 50, fullName),
      getRepoStats(owner, repo),
    ]).then(([r, s]) => {
      if (cancelled) return
      setReviews(r.items)
      setStats(s)
    }).catch(e => {
      if (!cancelled) toast.error(e.message)
    }).finally(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [owner, repo])

  if (loading) {
    return (
      <div className="page-shell py-8">
        <div className="animate-pulse space-y-4">
          <div className="grid gap-4 md:grid-cols-[minmax(0,1.16fr)_18rem]">
            <div className="h-72 rounded-2xl bg-slate-200 dark:bg-slate-800" />
            <div className="h-72 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          </div>
          <div className="h-72 rounded-2xl bg-slate-200 dark:bg-slate-800" />
        </div>
      </div>
    )
  }

  const severityDist: Record<Severity, number> = stats?.severityDistribution ?? {
    CRITICAL: 0,
    HIGH: 0,
    MEDIUM: 0,
    LOW: 0,
    INFO: 0,
  }
  const maxSeverity = Math.max(...Object.values(severityDist), 1)
  const summaryItems = [
    { label: '审查次数', value: stats?.totalReviews ?? 0, hint: '累计结果' },
    { label: '发现问题', value: stats?.totalIssues ?? 0, hint: '仓库范围' },
    { label: '平均问题', value: (stats?.avgIssuesPerReview ?? 0).toFixed(1), hint: '每次审查' },
  ]

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card overflow-hidden">
        <div className="grid gap-0 md:grid-cols-[minmax(0,1.16fr)_18rem]">
          <div className="px-5 py-5 md:px-6 md:py-6">
            <div className="space-y-1">
              <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
              <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{fullName}</h1>
            </div>

            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              {summaryItems.map((item) => (
                <div key={item.label} className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4 dark:border-slate-800 dark:bg-slate-900/50">
                  <div className="flex items-end justify-between gap-3">
                    <div className="text-sm font-medium text-slate-500 dark:text-slate-400">{item.label}</div>
                    <div className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{item.value}</div>
                  </div>
                  <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">{item.hint}</div>
                </div>
              ))}
            </div>

            <div className="mt-5 rounded-2xl border border-slate-200 bg-white px-4 py-4 dark:border-slate-800 dark:bg-slate-950/60">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="text-sm font-medium text-slate-500 dark:text-slate-400">最近结果</div>
                  <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                    {reviews.length === 0 ? '还没有可回看的记录' : `${reviews.length} 条可追溯审查`}
                  </div>
                </div>
                <button
                  onClick={() => navigate('/')}
                  className="inline-flex items-center justify-center rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-800"
                >
                  审查此仓库
                </button>
              </div>
            </div>
          </div>

          <div className="border-t border-slate-200 bg-slate-50/70 px-5 py-5 dark:border-slate-800 dark:bg-slate-900/50 md:border-l md:border-t-0 md:px-6">
            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">严重度分布</div>
            <div className="mt-4 space-y-3">
              {(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'] as Severity[]).map(s => (
                <div key={s} className="space-y-2 rounded-xl bg-white px-4 py-3 dark:bg-slate-950/70">
                  <div className="flex items-center justify-between gap-3 text-sm">
                    <span className="font-medium text-slate-500 dark:text-slate-400">{s}</span>
                    <span className="text-slate-950 dark:text-white">{severityDist[s] || 0}</span>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                    <div
                      className="h-full rounded-full transition-[width] duration-700 ease-out"
                      style={{
                        width: `${maxSeverity > 0 ? ((severityDist[s] || 0) / maxSeverity) * 100 : 0}%`,
                        backgroundColor: SEVERITY_BAR_COLORS[s],
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="surface-card overflow-hidden">
        {reviews.length === 0 ? (
          <div className="px-5 py-10 text-center text-sm text-slate-400 dark:text-slate-500 md:px-6">
            暂无审查记录
          </div>
        ) : (
          <div className="divide-y divide-slate-200 dark:divide-slate-800">
            {reviews.map(r => {
              const issueCount = Object.values(r.analyses).reduce((sum, a) => sum + (a.issues?.length || 0), 0)
              return (
                <Link
                  key={r.id}
                  to={`/review/${r.id}`}
                  className="flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-slate-50 dark:hover:bg-slate-950/50 md:px-6"
                >
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-medium text-slate-950 dark:text-white">
                      {r.prMetadata?.title || r.prUrl}
                    </div>
                    <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                      {new Date(r.createdAt).toLocaleDateString('zh-CN')} · {issueCount} issues
                    </div>
                  </div>
                  <StatusBadge status={r.status} />
                </Link>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}
