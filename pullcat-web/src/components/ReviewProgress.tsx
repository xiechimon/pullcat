import type { TaskState, AnalysisType, AnalysisResult, Severity } from '../types/review'

interface AnalysisTaskCardProps {
  task: TaskState
  result?: AnalysisResult | null
  isActive: boolean
  onClick: () => void
}

const SEVERITY_WEIGHT: Record<Severity, number> = {
  CRITICAL: 5,
  HIGH: 4,
  MEDIUM: 3,
  LOW: 2,
  INFO: 1,
}

function getHighestSeverity(result?: AnalysisResult | null): Severity | null {
  if (!result || !result.issues || result.issues.length === 0) return null
  return result.issues.reduce((max, issue) => {
    return SEVERITY_WEIGHT[issue.severity] > SEVERITY_WEIGHT[max] ? issue.severity : max
  }, result.issues[0].severity)
}

function getTaskStyle(task: TaskState, result?: AnalysisResult | null, isActive: boolean = false) {
  if (task.status === 'PENDING') {
    return {
      icon: <span className="animate-pulse">○</span>,
      color: 'text-gray-400 dark:text-gray-500',
      bg: isActive ? 'bg-gray-100 dark:bg-gray-800' : 'bg-white dark:bg-gray-900',
      border: isActive ? 'border-gray-400' : 'border-gray-200 dark:border-gray-800',
      ring: isActive ? 'ring-2 ring-gray-200' : ''
    }
  }
  
  if (task.status === 'RUNNING') {
    return {
      icon: (
        <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      ),
      color: 'text-[#047857]',
      bg: isActive ? 'bg-[#ecfdf5] dark:bg-[#064e3b]/30' : 'bg-white dark:bg-gray-900',
      border: 'border-transparent',
      ring: 'animate-breathing'
    }
  }

  if (task.status === 'FAILED') {
    return {
      icon: '✕',
      color: 'text-red-500',
      bg: isActive ? 'bg-red-50 dark:bg-red-900/30' : 'bg-white dark:bg-gray-900',
      border: isActive ? 'border-red-500' : 'border-red-200 dark:border-red-900/50',
      ring: isActive ? 'ring-2 ring-red-500/50' : ''
    }
  }

  // COMPLETED
  const maxSeverity = getHighestSeverity(result)
  
  if (!maxSeverity) {
    return {
      icon: '✓',
      color: 'text-emerald-500',
      bg: isActive ? 'bg-emerald-50 dark:bg-emerald-900/30' : 'bg-white dark:bg-gray-900',
      border: isActive ? 'border-emerald-500' : 'border-emerald-200 dark:border-emerald-900/50',
      ring: isActive ? 'ring-2 ring-emerald-500/50' : ''
    }
  }

  if (maxSeverity === 'CRITICAL' || maxSeverity === 'HIGH') {
    return {
      icon: '!',
      color: 'text-red-500',
      bg: isActive ? 'bg-red-50 dark:bg-red-900/30' : 'bg-white dark:bg-gray-900',
      border: isActive ? 'border-red-500' : 'border-red-200 dark:border-red-900/50',
      ring: isActive ? 'ring-2 ring-red-500/50' : ''
    }
  }

  if (maxSeverity === 'MEDIUM') {
    return {
      icon: '!',
      color: 'text-amber-500',
      bg: isActive ? 'bg-amber-50 dark:bg-amber-900/30' : 'bg-white dark:bg-gray-900',
      border: isActive ? 'border-amber-500' : 'border-amber-200 dark:border-amber-900/50',
      ring: isActive ? 'ring-2 ring-amber-500/50' : ''
    }
  }

  return {
    icon: 'i',
    color: 'text-blue-500',
    bg: isActive ? 'bg-blue-50 dark:bg-blue-900/30' : 'bg-white dark:bg-gray-900',
    border: isActive ? 'border-blue-500' : 'border-blue-200 dark:border-blue-900/50',
    ring: isActive ? 'ring-2 ring-blue-500/50' : ''
  }
}

function AnalysisTaskCard({ task, result, isActive, onClick }: AnalysisTaskCardProps) {
  const style = getTaskStyle(task, result, isActive)

  return (
    <button
      onClick={onClick}
      className={`flex flex-col items-center justify-center gap-2 p-3 rounded-xl border text-center transition-all duration-300 ${style.bg} ${style.border} ${style.ring} ${
        isActive
          ? 'shadow-md scale-[1.02]'
          : 'hover:shadow-sm hover:scale-[1.01]'
      }`}
    >
      <div className={`text-xl font-bold flex items-center justify-center h-6 ${style.color}`}>
        {style.icon}
      </div>
      <div className="flex flex-col items-center min-w-0 w-full px-1">
        <p className={`text-sm font-medium w-full truncate ${isActive ? style.color : 'text-gray-700 dark:text-gray-300'}`}>
          {task.label}
        </p>
        {task.model && (
          <p className="text-[11px] w-full truncate mt-0.5 opacity-70 text-gray-500 dark:text-gray-400">
            {task.model}
          </p>
        )}
      </div>
    </button>
  )
}

interface ReviewProgressProps {
  tasks: TaskState[]
  results?: Record<string, AnalysisResult | null>
  activeTaskType: AnalysisType | null
  onTaskSelect: (type: AnalysisType) => void
}

export function ReviewProgress({ tasks, results = {}, activeTaskType, onTaskSelect }: ReviewProgressProps) {
  const completed = tasks.filter((t) => t.status === 'COMPLETED' || t.status === 'FAILED').length
  const total = tasks.length

  return (
    <div className="w-full">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-sm font-medium text-gray-800 dark:text-gray-200">
          分析进度
        </span>
        <span className="text-sm text-gray-500 dark:text-gray-400">
          ({completed}/{total})
        </span>
      </div>
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
        {tasks.map((task) => (
          <AnalysisTaskCard
            key={task.name}
            task={task}
            result={results[task.name]}
            isActive={activeTaskType === task.name}
            onClick={() => onTaskSelect(task.name as AnalysisType)}
          />
        ))}
      </div>
    </div>
  )
}
