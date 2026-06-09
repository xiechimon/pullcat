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
    <div className="page-shell home-page">
      <section className="home-workbench animate-fade-up">
        <div className="surface-card home-review-panel animate-scale-in delay-100">
          <PRInput onSubmit={handleSubmit} loading={loading} />
        </div>
      </section>
    </div>
  )
}
