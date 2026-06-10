interface ActionBarProps {
  selectedCount: number
  totalCount: number
  publishing: boolean
  published: boolean
  disabled?: boolean
  onPublish: () => void
}

export function ActionBar({
  selectedCount,
  totalCount,
  publishing,
  published,
  disabled,
  onPublish,
}: ActionBarProps) {
  return (
    <div
      className="fixed bottom-0 left-0 right-0 px-4 py-3 z-50 animate-action-bar-in"
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
          {published ? (
            <span className="text-sm text-[#047857] font-medium animate-fade-in">
              已发布
            </span>
          ) : (
            <button
              onClick={onPublish}
              disabled={publishing || disabled}
              className="px-5 py-2 bg-[#047857] hover:bg-[#064e3b] disabled:opacity-30 disabled:cursor-not-allowed text-white text-sm font-medium rounded-lg transition-colors border-2 border-[#047857]"
              style={{ transition: 'box-shadow 0.1s, transform 0.1s, background-color 0.15s' }}
              onMouseDown={e => { if (!publishing && !disabled) { e.currentTarget.style.boxShadow = '0px 0px 0px 4px rgba(4,120,87,0.3)'; e.currentTarget.style.transform = 'scale(0.97)' } }}
              onMouseUp={e => { e.currentTarget.style.boxShadow = ''; e.currentTarget.style.transform = '' }}
              onMouseLeave={e => { e.currentTarget.style.boxShadow = ''; e.currentTarget.style.transform = '' }}
            >
              {publishing ? '发布中...' : disabled ? '分析中...' : '发布到 PR'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
