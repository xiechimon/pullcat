import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { PRInput } from '../components/PRInput'
import { createReview } from '../lib/api'

export function HomePage() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

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
      <section className="surface-card overflow-hidden home-entry-panel">
        <div className="grid gap-0 md:grid-cols-[minmax(0,1.22fr)_15rem]">
          <div className="px-5 py-5 md:px-6 md:py-6">
            <div className="space-y-1">
              <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
              <h2 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">贴入 PR，立即开始</h2>
            </div>

            <div className="mt-5">
              <PRInput onSubmit={handleSubmit} loading={loading} compact />
            </div>

            <div className="home-supporting-strip mt-4">
              <span className="home-supporting-strip__item">公开仓库</span>
              <span className="home-supporting-strip__item">即时开始</span>
              <span className="home-supporting-strip__item">实时审查</span>
            </div>
          </div>

          <div className="border-t border-slate-200 bg-slate-50/70 px-5 py-5 dark:border-slate-800 dark:bg-slate-900/50 md:border-l md:border-t-0 md:px-6">
            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">会话参数</div>
            <dl className="mt-4 space-y-3">
              <div className="border-b border-slate-200/80 pb-3 dark:border-slate-800">
                <div className="flex items-end justify-between gap-3">
                  <dt className="text-sm font-medium text-slate-500 dark:text-slate-400">来源</dt>
                  <dd className="text-base font-semibold text-slate-950 dark:text-white">GitHub PR</dd>
                </div>
              </div>

              <div className="border-b border-slate-200/80 pb-3 dark:border-slate-800">
                <div className="flex items-end justify-between gap-3">
                  <dt className="text-sm font-medium text-slate-500 dark:text-slate-400">创建</dt>
                  <dd className="text-base font-semibold text-slate-950 dark:text-white">即时</dd>
                </div>
              </div>

              <div>
                <div className="flex items-end justify-between gap-3">
                  <dt className="text-sm font-medium text-slate-500 dark:text-slate-400">落点</dt>
                  <dd className="text-base font-semibold text-slate-950 dark:text-white">审查页</dd>
                </div>
              </div>
            </dl>
          </div>
        </div>
      </section>
    </div>
  )
}
