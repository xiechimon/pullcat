import type { AnalysisStatus, TaskState } from '../types/review'

interface AnalysisTaskCardProps {
  task: TaskState
}

const STATUS_ICONS: Record<AnalysisStatus, string> = {
  PENDING: '○',
  RUNNING: '◌',
  COMPLETED: '✓',
  FAILED: '✕',
}

const STATUS_COLORS: Record<AnalysisStatus, string> = {
  PENDING: '',
  RUNNING: 'text-[#047857] animate-pulse',
  COMPLETED: 'text-[#047857]',
  FAILED: 'text-red-500',
}

function AnalysisTaskCard({ task }: AnalysisTaskCardProps) {
  const colorClass = STATUS_COLORS[task.status]

  return (
    <div
      className="flex items-center gap-3 px-4 py-3 rounded-xl border"
      style={{
        backgroundColor: 'var(--color-surface)',
        borderColor: 'var(--color-border)',
      }}
    >
      <span className={`text-lg font-bold ${colorClass}`} style={task.status === 'PENDING' ? { color: 'var(--color-text-secondary)' } : undefined}>
        {STATUS_ICONS[task.status]}
      </span>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate" style={{ color: 'var(--color-text)' }}>
          {task.label}
        </p>
        {task.model && (
          <p className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>{task.model}</p>
        )}
      </div>
    </div>
  )
}

interface ReviewProgressProps {
  tasks: TaskState[]
}

export function ReviewProgress({ tasks }: ReviewProgressProps) {
  const completed = tasks.filter((t) => t.status === 'COMPLETED' || t.status === 'FAILED').length
  const total = tasks.length

  return (
    <div className="w-full">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-sm font-medium" style={{ color: 'var(--color-text)' }}>
          分析进度
        </span>
        <span className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
          ({completed}/{total})
        </span>
      </div>
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2">
        {tasks.map((task) => (
          <AnalysisTaskCard key={task.name} task={task} />
        ))}
      </div>
    </div>
  )
}
