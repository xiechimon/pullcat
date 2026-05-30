import { useReducer, useRef, useCallback, useEffect } from 'react'
import { createSSEConnection, createReview } from '../lib/api'
import type { AnalysisResult, AnalysisStatus, TaskState } from '../types/review'
import { TASK_LABELS, ANALYSIS_TYPES } from '../types/review'

interface ReviewState {
  reviewId: string | null
  loading: boolean
  isAnalyzing: boolean
  error: string | null
  tasks: TaskState[]
  results: Record<string, AnalysisResult | null>
}

type Action =
  | { type: 'START_SUBMIT' }
  | { type: 'SUBMIT_SUCCESS'; reviewId: string }
  | { type: 'SUBMIT_ERROR'; error: string }
  | { type: 'TASK_PROGRESS'; task: string; status: AnalysisStatus; model: string }
  | { type: 'TASK_RESULT'; result: AnalysisResult }
  | { type: 'ANALYSIS_COMPLETE' }
  | { type: 'TOGGLE_ISSUE'; issueId: string }
  | { type: 'SSE_CONNECT'; reviewId: string }

function createInitialTasks(): TaskState[] {
  return ANALYSIS_TYPES.map((name) => ({
    name,
    label: TASK_LABELS[name],
    status: 'PENDING' as AnalysisStatus,
    model: '',
    startedAt: null,
    completedAt: null,
  }))
}

function reviewReducer(state: ReviewState, action: Action): ReviewState {
  switch (action.type) {
    case 'START_SUBMIT':
      return {
        ...state,
        loading: true,
        isAnalyzing: true,
        error: null,
        tasks: createInitialTasks(),
        results: {},
      }

    case 'SUBMIT_SUCCESS':
      return {
        ...state,
        reviewId: action.reviewId,
        loading: true,
      }

    case 'SUBMIT_ERROR':
      return {
        ...state,
        loading: false,
        isAnalyzing: false,
        error: action.error,
      }

    case 'TASK_PROGRESS':
      return {
        ...state,
        tasks: state.tasks.map((t) =>
          t.name === action.task
            ? { ...t, status: action.status, model: action.model || t.model }
            : t
        ),
      }

    case 'TASK_RESULT':
      return {
        ...state,
        results: {
          ...state.results,
          [action.result.type.toLowerCase()]: action.result,
        },
      }

    case 'ANALYSIS_COMPLETE':
      return {
        ...state,
        loading: false,
        isAnalyzing: false,
      }

    case 'TOGGLE_ISSUE':
      return {
        ...state,
        results: Object.fromEntries(
          Object.entries(state.results).map(([key, result]) => {
            if (!result?.issues) return [key, result]
            return [
              key,
              {
                ...result,
                issues: result.issues.map((issue) =>
                  issue.id === action.issueId
                    ? { ...issue, selected: !issue.selected }
                    : issue
                ),
              },
            ]
          })
        ) as Record<string, AnalysisResult | null>,
      }

    case 'SSE_CONNECT':
      return {
        ...state,
        reviewId: action.reviewId,
      }

    default:
      return state
  }
}

function createInitialState(): ReviewState {
  return {
    reviewId: null,
    loading: false,
    isAnalyzing: false,
    error: null,
    tasks: createInitialTasks(),
    results: {},
  }
}

interface UseReviewReducerReturn {
  reviewId: string | null
  error: string | null
  loading: boolean
  isAnalyzing: boolean
  tasks: TaskState[]
  results: Record<string, AnalysisResult | null>
  startReview: (prUrl: string) => Promise<void>
  resumeReview: (reviewId: string, sseUrl: string) => void
  toggleIssue: (issueId: string) => void
}

export function useReviewReducer(): UseReviewReducerReturn {
  const [state, dispatch] = useReducer(reviewReducer, null, createInitialState)
  const eventSourceRef = useRef<EventSource | null>(null)

  const connectSSE = useCallback((sseUrl: string) => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const es = createSSEConnection(sseUrl)
    eventSourceRef.current = es

    es.addEventListener('connected', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      dispatch({ type: 'SSE_CONNECT', reviewId: data.reviewId })
    })

    es.addEventListener('analysis_started', () => {
      // Reset task statuses handled in START_SUBMIT
    })

    es.addEventListener('task_progress', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      dispatch({
        type: 'TASK_PROGRESS',
        task: data.task,
        status: data.status as AnalysisStatus,
        model: data.model || '',
      })
    })

    es.addEventListener('task_result', (event) => {
      const data = JSON.parse((event as MessageEvent).data) as AnalysisResult
      dispatch({ type: 'TASK_RESULT', result: data })
    })

    es.addEventListener('all_complete', () => {
      dispatch({ type: 'ANALYSIS_COMPLETE' })
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }
    })

    es.onerror = () => {
      // SSE会自动重试，不需要手动处理
    }
  }, [])

  const startReview = useCallback(async (prUrl: string) => {
    dispatch({ type: 'START_SUBMIT' })

    try {
      const response = await createReview(prUrl)
      dispatch({ type: 'SUBMIT_SUCCESS', reviewId: response.reviewId })
      connectSSE(response.sseUrl)
    } catch (e) {
      dispatch({
        type: 'SUBMIT_ERROR',
        error: e instanceof Error ? e.message : 'Failed to start review',
      })
    }
  }, [connectSSE])

  const resumeReview = useCallback((id: string, sseUrl: string) => {
    dispatch({ type: 'START_SUBMIT' })
    dispatch({ type: 'SUBMIT_SUCCESS', reviewId: id })
    connectSSE(sseUrl)
  }, [connectSSE])

  const toggleIssue = useCallback((issueId: string) => {
    dispatch({ type: 'TOGGLE_ISSUE', issueId })
  }, [])

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
      }
    }
  }, [])

  return {
    reviewId: state.reviewId,
    error: state.error,
    loading: state.loading,
    isAnalyzing: state.isAnalyzing,
    tasks: state.tasks,
    results: state.results,
    startReview,
    resumeReview,
    toggleIssue,
  }
}
