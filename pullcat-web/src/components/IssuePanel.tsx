import { useState, useMemo } from 'react'
import type { Issue, Severity } from '../types/review'
import { SEVERITY_COLORS } from '../types/review'

interface IssuePanelProps {
  issues: Issue[]
  activeIssueId?: string | null
  onToggle: (issueId: string) => void
  onSelectAll: () => void
  onDeselectAll: () => void
  onSelectHighAbove: () => void
  onIssueClick: (issueId: string) => void
}

type GroupMode = 'severity' | 'file' | 'none'

const SEVERITY_ORDER: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO']

export function IssuePanel({
  issues, activeIssueId, onToggle, onSelectAll, onDeselectAll, onSelectHighAbove, onIssueClick,
}: IssuePanelProps) {
  const [search, setSearch] = useState('')
  const [groupMode, setGroupMode] = useState<GroupMode>('severity')
  const [collapsed, setCollapsed] = useState<Set<string>>(() => new Set(['LOW', 'INFO']))

  const filtered = useMemo(() => {
    if (!search.trim()) return issues
    const q = search.toLowerCase()
    return issues.filter(i =>
      i.title.toLowerCase().includes(q) ||
      i.description?.toLowerCase().includes(q) ||
      (i.file || '').toLowerCase().includes(q)
    )
  }, [issues, search])

  const groups = useMemo(() => {
    if (groupMode === 'none') return [{ key: 'all', label: '全部', issues: filtered }]

    const map = new Map<string, Issue[]>()
    for (const issue of filtered) {
      const key = groupMode === 'severity' ? issue.severity : (issue.file || '未知文件')
      const existing = map.get(key) || []
      existing.push(issue)
      map.set(key, existing)
    }

    if (groupMode === 'severity') {
      return SEVERITY_ORDER.filter(s => map.has(s)).map(s => ({
        key: s, label: s, issues: map.get(s)!,
      }))
    }
    return Array.from(map.entries()).map(([key, issues]) => ({ key, label: key, issues }))
  }, [filtered, groupMode])

  const toggleCollapse = (key: string) => {
    setCollapsed(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  const selectedCount = issues.filter(i => i.selected).length

  return (
    <div className="flex flex-col h-full border border-gray-200 dark:border-gray-700 rounded-xl bg-white dark:bg-slate-900 overflow-hidden">
      <div className="p-3 border-b border-gray-200 dark:border-gray-700 space-y-2">
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="搜索问题..."
          className="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-slate-800"
        />
        <div className="flex items-center gap-1 flex-wrap">
          <button onClick={onSelectAll} className="text-xs px-2 py-1 rounded border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800">
            全选
          </button>
          <button onClick={onDeselectAll} className="text-xs px-2 py-1 rounded border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800">
            取消全选
          </button>
          <button onClick={onSelectHighAbove} className="text-xs px-2 py-1 rounded border border-gray-300 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800">
            仅选 HIGH+
          </button>
          <span className="ml-auto text-xs text-gray-500">{selectedCount}/{issues.length}</span>
        </div>
        <div className="flex gap-1">
          {(['severity', 'file', 'none'] as GroupMode[]).map(m => (
            <button
              key={m}
              onClick={() => setGroupMode(m)}
              className={`text-xs px-2 py-0.5 rounded ${groupMode === m ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800'}`}
            >
              {m === 'severity' ? '按严重度' : m === 'file' ? '按文件' : '列表'}
            </button>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {filtered.length === 0 ? (
          <div className="p-6 text-center text-sm text-gray-500">暂无匹配问题</div>
        ) : (
          groups.map(group => {
            const isCollapsed = collapsed.has(group.key)
            return (
              <div key={group.key}>
                <button
                  onClick={() => toggleCollapse(group.key)}
                  className="w-full flex items-center gap-2 px-3 py-2 text-left text-sm font-medium bg-gray-50 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-750 transition-colors"
                >
                  <span className="text-xs text-gray-400">{isCollapsed ? '+' : '-'}</span>
                  <span>{group.label}</span>
                  <span className="text-xs text-gray-400 ml-auto">
                    {group.issues.filter(i => i.selected).length}/{group.issues.length}
                  </span>
                </button>
                {!isCollapsed && group.issues.map(issue => (
                  <div
                    key={issue.id}
                    onClick={() => onIssueClick(issue.id)}
                    className={`flex items-start gap-2 px-3 py-2 border-b border-gray-100 dark:border-gray-800 cursor-pointer transition-colors ${
                      issue.id === activeIssueId
                        ? 'bg-yellow-50 dark:bg-yellow-950/20'
                        : 'hover:bg-gray-50 dark:hover:bg-gray-800'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={issue.selected}
                      onChange={e => { e.stopPropagation(); onToggle(issue.id) }}
                      className="mt-1"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className={`px-1.5 py-0.5 rounded text-[10px] font-medium ${SEVERITY_COLORS[issue.severity]}`}>
                          {issue.severity}
                        </span>
                        {issue.confidence !== undefined && (
                          <span className="text-[10px] text-gray-400">{Math.round(issue.confidence * 100)}%</span>
                        )}
                        {issue.sourceDimensions && issue.sourceDimensions.length > 1 && (
                          <span className="text-[10px] text-purple-500">多源</span>
                        )}
                      </div>
                      <div className="text-xs text-gray-700 dark:text-gray-300 mt-0.5 truncate">{issue.title}</div>
                      <div className="text-[10px] text-gray-400 mt-0.5">
                        {issue.file}{issue.line ? `:${issue.line}` : ''}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
