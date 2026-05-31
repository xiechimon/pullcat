import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { StatusBadge } from '../components/StatusBadge'
import type { StatsOverview, ReviewSession, Severity } from '../types/review'
import { SEVERITY_BAR_COLORS } from '../types/review'
import { getStatsOverview, getReviews } from '../lib/api'

export function DashboardPage() {
  const [stats, setStats] = useState<StatsOverview | null>(null)
  const [recentReviews, setRecentReviews] = useState<ReviewSession[]>([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)
    Promise.all([
      getStatsOverview(),
      getReviews(0, 10),
    ]).then(([s, r]) => {
      if (cancelled) return
      setStats(s)
      setRecentReviews(r.items)
    }).catch(e => {
      if (!cancelled) toast.error(e.message)
    }).finally(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  }, [])

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 w-48 bg-gray-200 dark:bg-gray-700 rounded" />
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-24 bg-gray-200 dark:bg-gray-700 rounded-xl" />
            ))}
          </div>
        </div>
      </div>
    )
  }

  const totalIssues = stats?.totalIssues ?? 0
  const severityDist = stats?.severityDistribution ?? ({} as Record<Severity, number>)
  const maxSeverity = Math.max(...Object.values(severityDist), 1)

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">仪表盘</h1>
        <button
          onClick={() => navigate('/')}
          className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors text-sm font-medium"
        >
          + 新建审查
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: '总审查次数', value: stats?.totalReviews ?? 0 },
          { label: '发现问题总数', value: totalIssues },
          { label: '覆盖仓库数', value: stats?.repoCount ?? 0 },
          { label: '平均问题数', value: ((stats?.avgIssuesPerReview ?? 0)).toFixed(1) },
        ].map(card => (
          <div key={card.label} className="p-4 bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">{card.value}</div>
            <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">{card.label}</div>
          </div>
        ))}
      </div>

      <div className="p-6 bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">严重度分布</h2>
        <div className="space-y-3">
          {(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'] as Severity[]).map(s => (
            <div key={s} className="flex items-center gap-3">
              <span className="w-16 text-sm text-gray-600 dark:text-gray-400">{s}</span>
              <div className="flex-1 h-5 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full transition-all"
                  style={{
                    width: `${maxSeverity > 0 ? ((severityDist[s] || 0) / maxSeverity) * 100 : 0}%`,
                    backgroundColor: SEVERITY_BAR_COLORS[s],
                  }}
                />
              </div>
              <span className="w-10 text-sm text-right text-gray-600 dark:text-gray-400">{severityDist[s] || 0}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">最近审查</h2>
        </div>
        {recentReviews.length === 0 ? (
          <div className="p-8 text-center text-gray-500 dark:text-gray-400">
            暂无审查记录，<Link to="/" className="text-emerald-600 hover:underline">开始第一次审查</Link>
          </div>
        ) : (
          <div className="divide-y divide-gray-100 dark:divide-gray-700">
            {recentReviews.map(r => {
              const issueCount = Object.values(r.analyses).reduce((sum, a) => sum + (a.issues?.length || 0), 0)
              return (
                <Link
                  key={r.id}
                  to={`/review/${r.id}`}
                  className="flex items-center justify-between p-4 hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors"
                >
                  <div className="min-w-0 flex-1">
                    <div className="text-sm font-medium text-gray-900 dark:text-white truncate">
                      {r.prMetadata?.title || r.prUrl}
                    </div>
                    <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      {r.repositoryFullName || '-'} · {new Date(r.createdAt).toLocaleDateString('zh-CN')}
                    </div>
                  </div>
                  <div className="flex items-center gap-3 ml-4">
                    <span className="text-xs text-gray-500">{issueCount} issues</span>
                    <StatusBadge status={r.status} />
                  </div>
                </Link>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
