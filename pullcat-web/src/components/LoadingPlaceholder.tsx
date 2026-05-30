import type { TaskState } from '../types/review'

interface LoadingPlaceholderProps {
  task: TaskState
}

export function LoadingPlaceholder({ task }: LoadingPlaceholderProps) {
  if (task.status === 'RUNNING') {
    return (
      <div className="rounded-xl p-16 flex flex-col items-center justify-center border-2 border-dashed border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/30 space-y-5 transition-all duration-300">
        <svg className="animate-spin h-10 w-10 text-emerald-700" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
        </svg>
        <p className="text-gray-500 dark:text-gray-400 font-medium animate-pulse text-lg">正在分析 {task.label}...</p>
      </div>
    )
  }

  if (task.status === 'PENDING') {
    return (
      <div className="rounded-xl p-16 flex flex-col items-center justify-center border-2 border-dashed border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/30 space-y-5 transition-all duration-300">
        <div className="h-10 w-10 text-gray-300 dark:text-gray-600 flex items-center justify-center text-3xl">○</div>
        <p className="text-gray-400 dark:text-gray-500 font-medium text-lg">等待分析 {task.label}...</p>
      </div>
    )
  }

  if (task.status === 'FAILED') {
    return (
      <div className="rounded-xl p-16 flex flex-col items-center justify-center border-2 border-dashed border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/30 space-y-5 transition-all duration-300">
        <p className="text-red-500 font-medium text-lg">分析 {task.label} 失败</p>
      </div>
    )
  }

  return (
    <div className="rounded-xl p-16 flex flex-col items-center justify-center border-2 border-dashed border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/30 space-y-5 transition-all duration-300">
      <p className="text-gray-500 dark:text-gray-400 font-medium text-lg">准备中...</p>
    </div>
  )
}
