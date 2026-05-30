import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import type { ReviewSession, Severity } from '../types/review'
import { SEVERITY_BAR_COLORS } from '../types/review'
import { getRepoStats, getReviews } from '../lib/api'

export function RepoPage() {
  const { owner, repo } = useParams<{ owner: string; repo: string }>()
  const [reviews, setReviews] = useState<ReviewSession[]>([])
  const [stats, setStats] = useState<Record<string, unknown> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
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
      if (!cancelled) setError(e.message)
    }).finally(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [owner, repo])

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8 animate-pulse space-y-4">
        <div className="h-8 w-64 bg-gray-200 dark:bg-gray-700 rounded" />
        <div className="h-24 bg-gray-200 dark:bg-gray-700 rounded-xl" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-8">
        <div className="p-6 border border-red-200 bg-red-50 dark:bg-red-950/30 rounded-xl text-red-700 dark:text-red-400">
          {error}
        </div>
      </div>
    )
  }

  const severityDist = (stats?.severityDistribution as Record<string, number>) || {}
  const maxSeverity = Math.max(...Object.values(severityDist), 1)

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{fullName}</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            {(stats?.totalReviews as number) ?? 0} 次审查 · {(stats?.totalIssues as number) ?? 0} 个问题
          </p>
        </div>
        <button
          onClick={() => navigate('/')}
          className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors text-sm font-medium"
        >
          审查此仓库
        </button>
      </div>

      <div className="grid grid-cols-3 gap-4">
        {[
          { label: '审查次数', value: (stats?.totalReviews as number) ?? 0 },
          { label: '发现问题', value: (stats?.totalIssues as number) ?? 0 },
          { label: '平均问题数', value: ((stats?.avgIssuesPerReview as number) ?? 0).toFixed(1) },
        ].map(c => (
          <div key={c.label} className="p-4 bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">{c.value}</div>
            <div className="text-sm text-gray-500 dark:text-gray-400 mt-1">{c.label}</div>
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
                  className="h-full rounded-full"
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
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">审查记录</h2>
        </div>
        {reviews.length === 0 ? (
          <div className="p-8 text-center text-gray-500 dark:text-gray-400">暂无审查记录</div>
        ) : (
          <div className="divide-y divide-gray-100 dark:divide-gray-700">
            {reviews.map(r => {
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
                      {new Date(r.createdAt).toLocaleDateString('zh-CN')} · {issueCount} issues
                    </div>
                  </div>
                  <StatusBadge status={r.status} />
                </Link>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    FAILED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
    PUBLISHED: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  }
  const labels: Record<string, string> = { COMPLETED: '已完成', FAILED: '失败', PUBLISHED: '已发布' }
  return (
    <span className={`px-2 py-0.5 rounded text-xs font-medium ml-3 ${colors[status] || 'bg-gray-100 text-gray-600'}`}>
      {labels[status] || status}
    </span>
  )
}
