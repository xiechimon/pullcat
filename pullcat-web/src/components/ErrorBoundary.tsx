import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'

interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('ErrorBoundary caught:', error, errorInfo)
  }

  handleRetry = (): void => {
    this.setState({ hasError: false, error: null })
  }

  render(): ReactNode {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <div className="flex items-center justify-center min-h-[400px] p-8">
          <div className="text-center max-w-md">
            <div className="text-4xl mb-4 text-gray-300 dark:text-gray-600">!</div>
            <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-200 mb-2">
              页面出现错误
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-6 break-all">
              {this.state.error?.message || '未知错误'}
            </p>
            <button
              onClick={this.handleRetry}
              className="px-5 py-2 bg-emerald-700 hover:bg-emerald-800 text-white font-medium rounded-lg transition-colors text-sm"
            >
              重试
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
