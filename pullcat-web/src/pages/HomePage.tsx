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
    <div className="page-shell py-8 animate-fade-in">
      <section className="surface-card px-5 py-6 md:px-6 md:py-7">
        <div className="max-w-3xl space-y-5">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white md:text-3xl">
            开始审查
          </h1>

          <PRInput onSubmit={handleSubmit} loading={loading} compact />

          <div className="flex flex-wrap gap-x-4 gap-y-2 text-xs text-slate-500 dark:text-slate-400">
            <span>GitHub PR</span>
            <span>即时开始</span>
            <span>实时更新</span>
          </div>
        </div>
      </section>
    </div>
  )
}
