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
          <p className="home-hero__eyebrow">Pullcat Review Workspace</p>
          <h1 className="home-hero__title">开始一次新审查</h1>
          <p className="home-hero__summary">
            粘贴 GitHub PR 链接后，Pullcat 会抓取变更、组织多维度分析结果，并把确认后的评论发布回原始 Pull Request
          </p>
        </div>

        <div className="surface-card home-review-panel animate-scale-in delay-100">
          <PRInput onSubmit={handleSubmit} loading={loading} />
        </div>
      </section>

      <section className="home-flow animate-fade-up delay-150" aria-label="审查流程">
        <div className="home-flow__item">
          <span className="home-flow__step">01</span>
          <div>
            <h2 className="home-flow__title">粘贴 PR 链接</h2>
            <p className="home-flow__description">直接输入 GitHub Pull Request URL，作为本次审查的唯一入口</p>
          </div>
        </div>
        <div className="home-flow__item">
          <span className="home-flow__step">02</span>
          <div>
            <h2 className="home-flow__title">等待 AI 分析</h2>
            <p className="home-flow__description">系统会自动拉取代码差异，生成问题列表、建议和审查结论</p>
          </div>
        </div>
        <div className="home-flow__item">
          <span className="home-flow__step">03</span>
          <div>
            <h2 className="home-flow__title">确认并发布评论</h2>
            <p className="home-flow__description">你可以在审查页逐条确认问题，再把结果发布回 Pull Request</p>
          </div>
        </div>
      </section>
    </div>
  )
}
