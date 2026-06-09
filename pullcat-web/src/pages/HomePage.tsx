import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { PRInput } from '../components/PRInput'
import { StatusBadge } from '../components/StatusBadge'
import { createReview, getReviews } from '../lib/api'
import type { ReviewSessionRespDTO } from '../types/review'

function getIssueCount(review: ReviewSessionRespDTO) {
  return Object.values(review.analyses).reduce((sum, analysis) => sum + (analysis.issues?.length || 0), 0)
}

export function HomePage() {
  const [loading, setLoading] = useState(false)
  const [reviewsLoading, setReviewsLoading] = useState(true)
  const [recentReviews, setRecentReviews] = useState<ReviewSessionRespDTO[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false

    setReviewsLoading(true)
    setErrorMessage(null)

    getReviews(0, 10)
      .then((response) => {
        if (!cancelled) setRecentReviews(response.items)
      })
      .catch((error) => {
        if (cancelled) return
        const message = error instanceof Error ? error.message : '加载最近审查失败'
        setErrorMessage(message)
        toast.error(message)
      })
      .finally(() => {
        if (!cancelled) setReviewsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const handleSubmit = async (prUrl: string) => {
    setLoading(true)
    try {
      const response = await createReview(prUrl)
      navigate(`/review/${response.reviewId}`, {
        state: { reviewId: response.reviewId, sseUrl: response.sseUrl },
      })
    } catch (e) {
      setLoading(false)
      toast.error(e instanceof Error ? e.message : '请求后端服务失败，请检查网络或后端状态')
    }
  }

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card px-4 py-4 sm:px-5 md:px-6">
        <div>
          <PRInput onSubmit={handleSubmit} loading={loading} compact />
        </div>
      </section>

      {errorMessage && (
        <section className="flex flex-col gap-3 rounded-2xl border border-amber-200 bg-amber-50/80 px-5 py-4 md:flex-row md:items-center md:justify-between dark:border-amber-900/40 dark:bg-amber-950/20">
          <div className="space-y-1">
            <h2 className="text-sm font-semibold text-amber-900 dark:text-amber-200">最近审查暂时不可用</h2>
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

        {reviewsLoading ? (
          <div className="px-5 py-5 md:px-6">
            <div className="animate-pulse space-y-3">
              {[1, 2, 3].map((item) => (
                <div key={item} className="h-20 rounded-2xl bg-slate-200 dark:bg-slate-800" />
              ))}
            </div>
          </div>
        ) : recentReviews.length === 0 ? (
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
