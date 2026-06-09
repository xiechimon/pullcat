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
      <section className="home-hero animate-fade-up">
        <div className="home-hero__intro">
          <h1 className="home-hero__title">开始审查</h1>
          <p className="home-hero__summary">输入一个 GitHub Pull Request 链接</p>
        </div>

        <div className="surface-card home-review-panel animate-scale-in delay-100">
          <PRInput onSubmit={handleSubmit} loading={loading} />
        </div>

        <div className="home-supporting-strip animate-fade-in delay-150" aria-label="辅助信息">
          <span className="home-supporting-strip__item">支持公开仓库 PR</span>
          <span className="home-supporting-strip__item">创建后自动进入审查页</span>
        </div>
      </section>
    </div>
  )
}
