import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { compareReviews } from '../lib/api'
import type { CompareReviewsRespDTO } from '../types/review'

export function ComparePage() {
  const [searchParams] = useSearchParams()
  const r1 = searchParams.get('r1')
  const r2 = searchParams.get('r2')

  const [data, setData] = useState<CompareReviewsRespDTO | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!r1 || !r2) return

    let cancelled = false
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)

    compareReviews(r1, r2)
      .then((result) => {
        if (cancelled) return
        setData(result)
        setLoading(false)
      })
      .catch((e) => {
        if (cancelled) return
        toast.error(e instanceof Error ? e.message : '对比失败，请返回重试')
        setLoading(false)
      })

    return () => { cancelled = true }
  }, [r1, r2])

  if (!r1 || !r2) {
    return (
      <div className="page-shell py-8">
        <section className="surface-card px-5 py-10 text-center md:px-6">
          <div className="mx-auto max-w-md space-y-4">
            <div className="space-y-1">
              <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
              <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">选择两次审查再比较</h1>
            </div>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              从历史记录里勾选两次结果，这里只负责看变化。
            </p>
            <Link
              to="/history"
              className="inline-flex items-center justify-center rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-800"
            >
              前往审查历史
            </Link>
          </div>
        </section>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="page-shell py-8">
        <div className="animate-pulse space-y-4">
          <div className="grid gap-4 md:grid-cols-[minmax(0,1.12fr)_18rem]">
            <div className="h-64 rounded-2xl bg-slate-200 dark:bg-slate-800" />
            <div className="h-64 rounded-2xl bg-slate-200 dark:bg-slate-800" />
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-28 rounded-2xl bg-slate-200 dark:bg-slate-800" />
            ))}
          </div>
        </div>
      </div>
    )
  }

  if (!data) return null

  const issueDelta = (data.totalIssues2 ?? 0) - (data.totalIssues1 ?? 0)
  const reviewCards = [
    {
      label: 'Review A',
      url: data.review1?.prUrl || r1,
      value: data.totalIssues1 ?? '-',
      hint: '基线结果',
    },
    {
      label: 'Review B',
      url: data.review2?.prUrl || r2,
      value: data.totalIssues2 ?? '-',
      hint: '当前结果',
    },
  ]

  const changeCards = [
    { label: '新引入', value: data.newCount ?? 0, tone: 'text-red-600 dark:text-red-400', note: '当前新增' },
    { label: '已修复', value: data.fixedCount ?? 0, tone: 'text-emerald-600 dark:text-emerald-400', note: '当前消失' },
    { label: '持续存在', value: data.persistentCount ?? 0, tone: 'text-amber-600 dark:text-amber-400', note: '两次都在' },
  ]

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card overflow-hidden">
        <div className="grid gap-0 md:grid-cols-[minmax(0,1.12fr)_18rem]">
          <div className="px-5 py-5 md:px-6 md:py-6">
            <div className="space-y-1">
              <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
              <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">两次审查，差异一眼看清</h1>
            </div>

            <div className="mt-4 grid gap-3 md:grid-cols-2">
              {reviewCards.map((card) => (
                <div key={card.label} className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4 dark:border-slate-800 dark:bg-slate-900/50">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="text-sm font-medium text-slate-500 dark:text-slate-400">{card.label}</div>
                      <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">{card.hint}</div>
                    </div>
                    <div className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{card.value}</div>
                  </div>
                  <a
                    href={card.url || '#'}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="mt-4 block break-all text-sm font-medium text-emerald-700 hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300"
                  >
                    {card.url}
                  </a>
                </div>
              ))}
            </div>

            <div className="mt-4 rounded-2xl border border-slate-200 bg-white px-4 py-4 dark:border-slate-800 dark:bg-slate-950/60">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="text-sm font-medium text-slate-500 dark:text-slate-400">问题总数变化</div>
                  <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">只看净变化，不重复解释</div>
                </div>
                <div className="flex items-center gap-3 text-slate-950 dark:text-white">
                  <span className="text-xl font-semibold">{data.totalIssues1 ?? '-'}</span>
                  <span className="text-sm text-slate-400">
                    {issueDelta > 0 ? '↗' : issueDelta < 0 ? '↘' : '→'}
                  </span>
                  <span className="text-xl font-semibold">{data.totalIssues2 ?? '-'}</span>
                  <span className={`text-sm font-medium ${issueDelta > 0 ? 'text-red-600 dark:text-red-400' : issueDelta < 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-400 dark:text-slate-500'}`}>
                    {issueDelta > 0 ? '+' : ''}{issueDelta}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="border-t border-slate-200 bg-slate-50/70 px-5 py-5 dark:border-slate-800 dark:bg-slate-900/50 md:border-l md:border-t-0 md:px-6">
            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">变化拆分</div>
            <div className="mt-4 space-y-3">
              {changeCards.map((card) => (
                <div key={card.label} className="rounded-xl bg-white px-4 py-4 dark:bg-slate-950/70">
                  <div className="flex items-end justify-between gap-3">
                    <div>
                      <div className="text-sm font-medium text-slate-500 dark:text-slate-400">{card.label}</div>
                      <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">{card.note}</div>
                    </div>
                    <div className={`text-2xl font-semibold tracking-tight ${card.tone}`}>{card.value}</div>
                  </div>
                </div>
              ))}
            </div>

            <Link
              to="/history"
              className="mt-4 inline-flex text-sm font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-300"
            >
              返回审查历史
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
