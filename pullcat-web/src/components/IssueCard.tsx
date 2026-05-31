import { useState } from 'react'
import type { Severity } from '../types/review'
import { SEVERITY_COLORS, SEVERITY_BG } from '../types/review'

interface IssueCardProps {
  id: string
  severity: Severity
  file: string | null
  line: number | null
  title: string
  description: string
  suggestion: string
  suggestionCode?: string | null
  confidence: number
  selected: boolean
  onToggle: (id: string) => void
  feedback?: string | null
  feedbackReason?: string | null
  onFeedback?: (id: string, accepted: boolean, reason?: string) => void
}

const SEVERITY_LABELS: Record<Severity, string> = {
  CRITICAL: '严重',
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
  INFO: '提示',
}

export function IssueCard({
  id, severity, file, line, title, description, suggestion, suggestionCode, confidence, selected, onToggle, feedback, feedbackReason, onFeedback,
}: IssueCardProps) {
  const [showReason, setShowReason] = useState(false)
  const [reason, setReason] = useState('')

  const hasFeedback = feedback === 'ACCEPTED' || feedback === 'REJECTED'

  const getBorderClass = () => {
    if (feedback === 'ACCEPTED') return 'border-l-4 border-l-emerald-500'
    if (feedback === 'REJECTED') return 'border-l-4 border-l-red-500'
    return ''
  }

  const handleAccept = () => {
    if (onFeedback) onFeedback(id, true)
  }

  const handleReject = () => {
    if (!showReason) {
      setShowReason(true)
      return
    }
    if (onFeedback) onFeedback(id, false, reason || undefined)
    setShowReason(false)
    setReason('')
  }

  const handleReasonKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      handleReject()
    }
    if (e.key === 'Escape') {
      setShowReason(false)
      setReason('')
    }
  }

  return (
    <div className={`border rounded-lg p-4 transition-colors ${SEVERITY_BG[severity]} ${selected ? 'ring-2 ring-amber-400' : ''} ${getBorderClass()}`}> 
      <div className="flex items-start gap-3">
        <input
          type="checkbox"
          checked={selected}
          onChange={() => onToggle(id)}
          className="mt-1 h-4 w-4 rounded border-gray-300 text-amber-500 focus:ring-amber-400"
        />
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${SEVERITY_COLORS[severity]}`}>
              {SEVERITY_LABELS[severity]}
            </span>
            {file && (
              <span className="text-xs font-mono" style={{ color: 'var(--color-text-secondary)' }}>
                {file}{line != null ? `:${line}` : ''}
              </span>
            )}
            <span className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>
              置信度: {(confidence * 100).toFixed(0)}%
            </span>
          </div>
          <h4 className="text-sm font-semibold mb-1" style={{ color: 'var(--color-text)' }}>
            {title}
          </h4>
          <p className="text-sm mb-2" style={{ color: 'var(--color-text-secondary)' }}>
            {description}
          </p>
          {suggestion && (
            <div
              className="text-sm rounded p-2 border"
              style={{
                color: 'var(--color-text)',
                backgroundColor: 'var(--color-bg)',
                borderColor: 'var(--color-border)',
              }}
            >
              <span className="font-medium">建议：</span>
              {suggestion}
            </div>
          )}
          {suggestionCode && (
            <div className="mt-2">
              <p className="text-xs mb-1" style={{ color: 'var(--color-text-secondary)' }}>
                 AI 生成的修复建议，请人工确认后应用
              </p>
              <pre
                className="text-sm rounded p-2 border overflow-x-auto"
                style={{
                  color: 'var(--color-text)',
                  backgroundColor: 'var(--color-bg)',
                  borderColor: 'var(--color-border)',
                }}
              >
                <code>{suggestionCode}</code>
              </pre>
            </div>
          )}
        </div>
      </div>

      {onFeedback && !hasFeedback && (
        <div className="flex items-center gap-2 pt-3 mt-3 border-t border-gray-200 dark:border-gray-700">
          <button
            onClick={handleAccept}
            className="text-xs px-2.5 py-1 rounded bg-emerald-50 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-400 border border-emerald-300 dark:border-emerald-700 hover:bg-emerald-100 dark:hover:bg-emerald-900/40 transition-colors"
          >
            接受 ✓
          </button>
          <button
            onClick={handleReject}
            className="text-xs px-2.5 py-1 rounded bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-400 border border-red-300 dark:border-red-700 hover:bg-red-100 dark:hover:bg-red-900/40 transition-colors"
          >
            {showReason ? '提交拒绝 ✗' : '拒绝 ✗'}
          </button>
          {showReason && (
            <>
              <input
                type="text"
                value={reason}
                onChange={e => setReason(e.target.value)}
                onKeyDown={handleReasonKeyDown}
                placeholder="拒绝原因（可选）"
                className="flex-1 text-xs px-2 py-1 rounded border border-gray-300 dark:border-gray-600 bg-white dark:bg-slate-800"
                autoFocus
              />
              <button
                onClick={() => { setShowReason(false); setReason('') }}
                className="text-xs text-gray-400 hover:text-gray-600"
              >
                取消
              </button>
            </>
          )}
        </div>
      )}

      {hasFeedback && (
        <div className={`flex items-center gap-2 pt-3 mt-3 border-t ${feedback === 'ACCEPTED' ? 'border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-400' : 'border-red-200 dark:border-red-800 text-red-700 dark:text-red-400'}`}>
          <span className="text-xs font-medium">
            {feedback === 'ACCEPTED' ? '✓ 已接受' : '✗ 已拒绝'}
          </span>
          {feedbackReason && (
            <span className="text-xs text-gray-500 dark:text-gray-400 truncate max-w-xs">
              — {feedbackReason}
            </span>
          )}
        </div>
      )}
    </div>
  )
}
