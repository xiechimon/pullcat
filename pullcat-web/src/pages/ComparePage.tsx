import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { compareReviews } from '../lib/api'

interface CompareData {
  review1?: { id: string; prUrl: string }
  review2?: { id: string; prUrl: string }
  newCount?: number
  fixedCount?: number
  persistentCount?: number
  totalIssues1?: number
  totalIssues2?: number
  error?: string
}

export function ComparePage() {
  const [searchParams] = useSearchParams()
  const r1 = searchParams.get('r1')
  const r2 = searchParams.get('r2')

  const [data, setData] = useState<CompareData | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!r1 || !r2) return

    let cancelled = false
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)
    setError(null)

    compareReviews(r1, r2)
      .then((result) => {
        if (cancelled) return
        if (result.error) {
          setError(result.error as string)
        } else {
          setData(result as CompareData)
        }
        setLoading(false)
      })
      .catch((e) => {
        if (cancelled) return
        setError(e instanceof Error ? e.message : '对比失败')
        setLoading(false)
      })

    return () => { cancelled = true }
  }, [r1, r2])

  if (!r1 || !r2) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">Review 对比</h1>
        <p className="text-gray-500 dark:text-gray-400 mb-6">
          请从审查历史中选择两次 Review 进行对比
        </p>
        <Link
          to="/history"
          className="inline-block px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors"
        >
          前往审查历史
        </Link>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-48" />
          <div className="h-32 bg-gray-200 dark:bg-gray-700 rounded-xl" />
          <div className="grid grid-cols-3 gap-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-24 bg-gray-200 dark:bg-gray-700 rounded-xl" />
            ))}
          </div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">对比失败</h1>
        <p className="text-red-600 dark:text-red-400 mb-6">{error}</p>
        <Link
          to="/history"
          className="inline-block px-4 py-2 text-emerald-600 hover:underline"
        >
          返回审查历史
        </Link>
      </div>
    )
  }

  if (!data) return null

  const issueDelta = (data.totalIssues2 ?? 0) - (data.totalIssues1 ?? 0)

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 space-y-6">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Review 对比</h1>

      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">Review A</div>
          <a
            href={data.review1?.prUrl || '#'}
            target="_blank"
            rel="noopener noreferrer"
            className="text-sm text-emerald-600 hover:underline font-medium break-all"
          >
            {data.review1?.prUrl || r1}
          </a>
          <div className="text-2xl font-bold mt-2 text-gray-900 dark:text-white">
            {data.totalIssues1 ?? '-'}
            <span className="text-xs font-normal text-gray-500 ml-1">个问题</span>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
          <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">Review B</div>
          <a
            href={data.review2?.prUrl || '#'}
            target="_blank"
            rel="noopener noreferrer"
            className="text-sm text-emerald-600 hover:underline font-medium break-all"
          >
            {data.review2?.prUrl || r2}
          </a>
          <div className="text-2xl font-bold mt-2 text-gray-900 dark:text-white">
            {data.totalIssues2 ?? '-'}
            <span className="text-xs font-normal text-gray-500 ml-1">个问题</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <div className="bg-white dark:bg-slate-800 rounded-xl border border-l-4 border-l-red-500 p-4">
          <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">新引入的问题</div>
          <div className="text-3xl font-bold text-red-600 dark:text-red-400">
            {data.newCount ?? 0}
          </div>
          <div className="text-xs text-gray-400 mt-1">Review B 中新出现</div>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-xl border border-l-4 border-l-emerald-500 p-4">
          <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">已修复的问题</div>
          <div className="text-3xl font-bold text-emerald-600 dark:text-emerald-400">
            {data.fixedCount ?? 0}
          </div>
          <div className="text-xs text-gray-400 mt-1">Review B 中已消失</div>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-xl border border-l-4 border-l-amber-500 p-4">
          <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">持续存在的问题</div>
          <div className="text-3xl font-bold text-amber-600 dark:text-amber-400">
            {data.persistentCount ?? 0}
          </div>
          <div className="text-xs text-gray-400 mt-1">两次均存在</div>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 text-center">
        <div className="text-sm text-gray-500 dark:text-gray-400 mb-1">问题总数变化</div>
        <div className="flex items-center justify-center gap-3">
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {data.totalIssues1 ?? '-'}
          </span>
          <span className="text-gray-400 text-xl">
            {issueDelta > 0 ? '↗' : issueDelta < 0 ? '↘' : '→'}
          </span>
          <span className="text-2xl font-bold text-gray-900 dark:text-white">
            {data.totalIssues2 ?? '-'}
          </span>
          {issueDelta !== 0 && (
            <span className={`text-sm font-medium ${issueDelta > 0 ? 'text-red-600' : 'text-emerald-600'}`}>
              {issueDelta > 0 ? '+' : ''}{issueDelta}
            </span>
          )}
        </div>
      </div>

      <div className="text-center">
        <Link
          to="/history"
          className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
        >
          ← 返回审查历史
        </Link>
      </div>
    </div>
  )
}
