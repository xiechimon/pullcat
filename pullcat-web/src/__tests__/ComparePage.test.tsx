/// <reference types="@testing-library/jest-dom" />
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { toast } from 'sonner'
import { ComparePage } from '../pages/ComparePage'

vi.mock('../lib/api', () => ({
  compareReviews: vi.fn(),
}))

import { compareReviews } from '../lib/api'
const mockCompareReviews = compareReviews as ReturnType<typeof vi.fn>

function renderComparePage(search = '') {
  return render(
    <MemoryRouter initialEntries={[`/compare${search}`]}>
      <ComparePage />
    </MemoryRouter>
  )
}

describe('ComparePage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('缺少 r1/r2 参数时提示前往历史页', () => {
    renderComparePage()
    expect(screen.getByText(/查看审查历史/)).toBeInTheDocument()
  })

  it('API 失败时调用 toast.error，不渲染全页错误标题', async () => {
    const toastSpy = vi.spyOn(toast, 'error')
    mockCompareReviews.mockRejectedValue(new Error('网络超时'))
    renderComparePage('?r1=1&r2=2')
    await waitFor(() => {
      expect(toastSpy).toHaveBeenCalledWith('网络超时')
    })
    expect(screen.queryByText('对比失败')).not.toBeInTheDocument()
  })

  it('API 成功时渲染对比结果', async () => {
    mockCompareReviews.mockResolvedValue({
      review1: { prUrl: 'https://github.com/a/b/pull/1' },
      review2: { prUrl: 'https://github.com/a/b/pull/2' },
      totalIssues1: 3,
      totalIssues2: 1,
      newCount: 0,
      fixedCount: 2,
      persistentCount: 1,
    })
    renderComparePage('?r1=1&r2=2')
    await waitFor(() => {
      expect(screen.getByText('对比结果')).toBeInTheDocument()
    })
    expect(screen.getByText('已修复')).toBeInTheDocument()
  })
})
