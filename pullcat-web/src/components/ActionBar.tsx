interface ActionBarProps {
  selectedCount: number
  totalCount: number
  publishing: boolean
  published: boolean
  publishError: string | null
  onPublish: () => void
}

export function ActionBar({
  selectedCount,
  totalCount,
  publishing,
  published,
  publishError,
  onPublish,
}: ActionBarProps) {
  return (
    <div
      className="fixed bottom-0 left-0 right-0 px-4 py-3 z-50"
      style={{
        backgroundColor: 'var(--color-surface)',
        borderTop: '1px solid var(--color-border)',
      }}
    >
      <div className="max-w-3xl mx-auto flex items-center justify-between">
        <div className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
          已选择 {selectedCount}/{totalCount} 个问题
        </div>

        <div className="flex items-center gap-3">
          {publishError && (
            <span className="text-sm text-red-600">{publishError}</span>
          )}
          {published ? (
            <span className="text-sm text-[#047857] font-medium">
              发布成功！
            </span>
          ) : (
            <button
              onClick={onPublish}
              disabled={publishing}
              className="px-5 py-2 bg-[#047857] hover:bg-[#064e3b] disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors border-2 border-[#047857]"
            >
              {publishing ? '发布中...' : '发布到 PR'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
