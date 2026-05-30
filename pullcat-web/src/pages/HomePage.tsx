import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PRInput } from '../components/PRInput'
import { createReview } from '../lib/api'

export function HomePage() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const handleSubmit = async (prUrl: string) => {
    setLoading(true)
    setError(null)
    try {
      const response = await createReview(prUrl)
      navigate(`/review/${response.reviewId}`, {
        state: { reviewId: response.reviewId, sseUrl: response.sseUrl },
      })
    } catch (e) {
      setLoading(false)
      setError(e instanceof Error ? e.message : '请求后端服务失败，请检查网络或后端状态')
    }
  }

  return (
    <>
      <div className="transition-all duration-700 ease-in-out flex flex-col items-center text-center px-4 overflow-hidden max-h-[500px] opacity-100 pt-28 md:pt-36 pb-12 mb-4">
        <h1 className="text-4xl md:text-5xl lg:text-6xl font-semibold font-serif text-emerald-700 leading-tight max-w-4xl mb-6 tracking-tight">
          AI 驱动的 Pull Request 代码审查助手
        </h1>
        <p className="text-lg md:text-xl font-serif text-gray-600 dark:text-gray-400 max-w-2xl mb-6 leading-relaxed">
          输入 GitHub PR 链接，自动获取代码变更并进行多维度 AI 分析，审查后一键发布到 PR。
        </p>
      </div>

      <div className="w-full px-4 mb-6">
        <div className="input-card mx-auto">
          <PRInput onSubmit={handleSubmit} loading={loading} />
          {error && (
            <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {error}
            </div>
          )}
        </div>
      </div>
    </>
  )
}
