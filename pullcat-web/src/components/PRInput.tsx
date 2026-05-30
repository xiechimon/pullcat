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
            className="px-6 py-3 bg-[#047857] hover:bg-[#064e3b] disabled:opacity-80 text-white font-medium rounded-lg transition-colors whitespace-nowrap border-2 border-[#047857] flex items-center justify-center gap-2 min-w-[120px]"
          >
            {loading ? (
              <>
                <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
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
        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>
    </form>
  )
}
