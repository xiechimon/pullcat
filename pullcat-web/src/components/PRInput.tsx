import { useState } from 'react'
import { toast } from 'sonner'

interface PRInputProps {
  onSubmit: (prUrl: string) => void
  loading: boolean
}

const PR_URL_PATTERN = /^https:\/\/github\.com\/[\w.-]+\/[\w.-]+\/pull\/\d+(\/.*)?$/

export function PRInput({ onSubmit, loading }: PRInputProps) {
  const [url, setUrl] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = url.trim()
    if (!trimmed) {
      toast.error('请输入 GitHub PR 链接')
      return
    }
    if (!PR_URL_PATTERN.test(trimmed)) {
      toast.error('无效的 GitHub PR 链接，格式应为：https://github.com/owner/repo/pull/number')
      return
    }
    onSubmit(trimmed)
  }

  return (
    <form onSubmit={handleSubmit} className="home-review-form">
      <div className="home-review-form__controls">
        <div className="home-review-form__field">
          <input
            id="pr-url"
            type="text"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://github.com/owner/repo/pull/123"
            className="home-review-form__input"
            disabled={loading}
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className="home-review-form__submit"
        >
          {loading ? (
            <>
              <svg className="h-5 w-5 animate-spin text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              审查中...
            </>
          ) : (
            '开始审查'
          )}
        </button>
      </div>
    </form>
  )
}
