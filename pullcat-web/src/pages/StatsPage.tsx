import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { SeverityChart } from '../components/SeverityChart'
import { getStatsOverview } from '../lib/api'
import type { Severity, StatsOverviewRespDTO } from '../types/review'

export function StatsPage() {
  const [stats, setStats] = useState<StatsOverviewRespDTO | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    getStatsOverview()
      .then((response) => {
        if (!cancelled) setStats(response)
      })
      .catch((error) => {
        if (!cancelled) toast.error(error instanceof Error ? error.message : '加载统计失败')
      })
      .finally(() => {
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
          <div className="h-48 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          <div className="h-72 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          <div className="h-80 rounded-2xl bg-slate-200 dark:bg-slate-800" />
        </div>
      </div>
    )
  }

  if (!stats) return null

  const issueTypeData = (stats.commonIssueTypes || []).map(item => ({
    name: item.type.length > 36 ? `${item.type.slice(0, 36)}...` : item.type,
    fullName: item.type,
    count: item.count,
  }))
  const maxIssueCount = Math.max(...issueTypeData.map(item => item.count), 1)

  const summaryItems = [
    { label: '总审查', value: stats.totalReviews },
    { label: '总问题', value: stats.totalIssues },
    { label: '覆盖仓库', value: stats.repoCount },
    { label: '平均问题', value: stats.avgIssuesPerReview.toFixed(1) },
  ]

  const severityList: Array<{ label: string; value: number; tone: string }> = [
    { label: '高严重度', value: (stats.severityDistribution?.CRITICAL ?? 0) + (stats.severityDistribution?.HIGH ?? 0), tone: 'bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-300' },
    { label: '中严重度', value: stats.severityDistribution?.MEDIUM ?? 0, tone: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-950/40 dark:text-yellow-300' },
    { label: '低严重度', value: (stats.severityDistribution?.LOW ?? 0) + (stats.severityDistribution?.INFO ?? 0), tone: 'bg-slate-100 text-slate-700 dark:bg-slate-900 dark:text-slate-300' },
  ]

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card px-5 py-5 md:px-6 md:py-6">
        <div className="space-y-5">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {summaryItems.map(item => (
              <div key={item.label} className="rounded-2xl bg-slate-50 px-4 py-4 dark:bg-slate-900/60">
                <div className="text-sm font-medium text-slate-500 dark:text-slate-400">{item.label}</div>
                <div className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{item.value}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_18rem]">
        <div className="surface-card px-5 py-5 md:px-6 md:py-6">
          <div className="space-y-3">
            <div className="text-sm font-medium text-slate-500 dark:text-slate-400">高频问题</div>
            {issueTypeData.length > 0 ? (
              <div className="space-y-3">
                {issueTypeData.map((item, index) => (
                  <div key={item.fullName} className="grid gap-2 sm:grid-cols-[minmax(0,15rem)_minmax(0,1fr)_3rem] sm:items-center">
                    <div className="min-w-0">
                      <div className="text-sm font-medium text-slate-950 dark:text-white" title={item.fullName}>
                        {item.name}
                      </div>
                    </div>
                    <div className="h-3 overflow-hidden rounded-full bg-slate-100 dark:bg-slate-900/70">
                      <div
                        className="h-full rounded-full bg-emerald-700 transition-[width] duration-500"
                        style={{ width: `${(item.count / maxIssueCount) * 100}%` }}
                      />
                    </div>
                    <div className="text-right text-sm font-semibold text-slate-500 dark:text-slate-400">
                      {item.count}
                    </div>
                    {index < issueTypeData.length - 1 && (
                      <div className="sm:col-span-3 h-px bg-slate-100 dark:bg-slate-900/70" />
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-6 py-10 text-center text-sm text-slate-400 dark:border-slate-700 dark:bg-slate-900/40">
                暂无问题类型数据
              </div>
            )}
          </div>
        </div>

        <div className="surface-card px-5 py-5 md:px-6 md:py-6">
          <div className="space-y-4">
            <div className="text-sm font-medium text-slate-500 dark:text-slate-400">严重度分布</div>
            <div className="flex justify-center">
              <SeverityChart distribution={(stats.severityDistribution || {}) as Record<Severity, number>} size={190} />
            </div>

            <div className="space-y-2">
              {severityList.map(item => (
                <div key={item.label} className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-3 dark:bg-slate-900/60">
                  <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-medium ${item.tone}`}>
                    {item.label}
                  </span>
                  <span className="text-lg font-semibold text-slate-950 dark:text-white">{item.value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
