import type { SessionStatus } from '../types/review'

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  FAILED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  PUBLISHED: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
  ANALYZING: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  FETCHING: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-400',
}

const STATUS_LABELS: Record<string, string> = {
  COMPLETED: '已完成',
  FAILED: '失败',
  PUBLISHED: '已发布',
  ANALYZING: '分析中',
  FETCHING: '获取中',
}

interface StatusBadgeProps {
  status: SessionStatus | string
  asDot?: boolean
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLORS[status] || 'bg-gray-100 text-gray-600'}`}>
      {STATUS_LABELS[status] || status}
    </span>
  )
}
