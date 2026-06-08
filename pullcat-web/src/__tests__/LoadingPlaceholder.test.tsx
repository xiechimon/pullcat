/// <reference types="@testing-library/jest-dom" />
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { LoadingPlaceholder } from '../components/LoadingPlaceholder'
import type { TaskStateRespDTO } from '../types/review'

function makeTask(status: TaskStateRespDTO['status']): TaskStateRespDTO {
  return { name: 'summary', label: '总结', status, model: '', startedAt: null, completedAt: null }
}

describe('LoadingPlaceholder', () => {
  it('FAILED: 渲染带 aria-label 的 ✕，不渲染文字错误信息', () => {
    render(<LoadingPlaceholder task={makeTask('FAILED')} />)
    expect(screen.getByLabelText('总结 分析失败')).toBeInTheDocument()
    expect(screen.queryByText(/错误|failed|失败/i)).not.toBeInTheDocument()
  })

  it('RUNNING: 渲染进行中文字，不渲染 ✕', () => {
    render(<LoadingPlaceholder task={makeTask('RUNNING')} />)
    expect(screen.getByText(/正在分析/)).toBeInTheDocument()
    expect(screen.queryByLabelText('总结 分析失败')).not.toBeInTheDocument()
  })

  it('PENDING: 渲染等待文字，不渲染 ✕', () => {
    render(<LoadingPlaceholder task={makeTask('PENDING')} />)
    expect(screen.getByText(/等待分析/)).toBeInTheDocument()
    expect(screen.queryByLabelText('总结 分析失败')).not.toBeInTheDocument()
  })
})
