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
  confidence: number
  selected: boolean
  onToggle: (id: string) => void
}

const SEVERITY_LABELS: Record<Severity, string> = {
  CRITICAL: '严重',
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
  INFO: '提示',
}

export function IssueCard({
  id, severity, file, line, title, description, suggestion, confidence, selected, onToggle,
}: IssueCardProps) {
  return (
    <div className={`border rounded-lg p-4 transition-colors ${SEVERITY_BG[severity]} ${selected ? 'ring-2 ring-amber-400' : ''}`}>
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
        </div>
      </div>
    </div>
  )
}
