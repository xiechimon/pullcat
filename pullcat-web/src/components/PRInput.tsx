import { useState } from 'react'

interface PRInputProps {
  onSubmit: (prUrl: string) => void
  loading: boolean
}

const PR_URL_PATTERN = /^https:\/\/github\.com\/[\w.-]+\/[\w.-]+\/pull\/\d+(\/.*)?$/

export function PRInput({ onSubmit, loading }: PRInputProps) {
  const [url, setUrl] = useState('')
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = url.trim()
    if (!trimmed) {
      setError('请输入 GitHub PR 链接')
      return
    }
    if (!PR_URL_PATTERN.test(trimmed)) {
      setError('无效的 GitHub PR 链接，格式应为：https://github.com/owner/repo/pull/number')
      return
    }
    setError(null)
    onSubmit(trimmed)
  }

  return (
    <form onSubmit={handleSubmit} className="w-full">
      <div className="flex flex-col gap-3">
        <label htmlFor="pr-url" className="text-sm font-medium" style={{ color: 'var(--color-text)' }}>
          GitHub Pull Request URL
        </label>
        <div className="flex gap-2">
          <input
            id="pr-url"
            type="text"
            value={url}
            onChange={(e) => { setUrl(e.target.value); setError(null) }}
            placeholder="粘贴 GitHub PR 链接，例如 https://github.com/owner/repo/pull/123"
            className="flex-1 px-4 py-3 rounded-lg text-base focus:outline-none focus:ring-2 focus:ring-[#047857] focus:border-transparent"
            style={{
              backgroundColor: 'var(--color-bg)',
              color: 'var(--color-text)',
              border: '1px solid var(--color-border)',
            }}
            disabled={loading}
          />
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-3 bg-[#047857] hover:bg-[#064e3b] disabled:opacity-50 text-white font-medium rounded-lg transition-colors whitespace-nowrap border-2 border-[#047857]"
          >
            {loading ? '分析中...' : '开始审查'}
          </button>
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>
    </form>
  )
}
