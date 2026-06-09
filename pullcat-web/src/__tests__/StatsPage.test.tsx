/// <reference types="@testing-library/jest-dom" />
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { StatsPage } from '../pages/StatsPage'

vi.mock('../lib/api', () => ({
  getStatsOverview: vi.fn(),
}))

import { getStatsOverview } from '../lib/api'

function renderStatsPage() {
  return render(
    <MemoryRouter>
      <StatsPage />
    </MemoryRouter>,
  )
}

describe('StatsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders statistics summary and charts', async () => {
    vi.mocked(getStatsOverview).mockResolvedValue({
      totalReviews: 12,
      totalIssues: 34,
      repoCount: 5,
      avgIssuesPerReview: 2.8,
      severityDistribution: {
        CRITICAL: 1,
        HIGH: 3,
        MEDIUM: 8,
        LOW: 12,
        INFO: 10,
      },
      commonIssueTypes: [
        { type: '空指针风险', count: 6 },
        { type: '缺少错误处理', count: 4 },
      ],
    })

    renderStatsPage()

    expect(await screen.findByText('统计只回答两个问题')).toBeInTheDocument()
    expect(screen.getByText('总审查')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('高频问题')).toBeInTheDocument()
    expect(screen.getByText('严重度分布')).toBeInTheDocument()
    expect(screen.getByText('高严重度')).toBeInTheDocument()
  })

  it('renders empty issue type state', async () => {
    vi.mocked(getStatsOverview).mockResolvedValue({
      totalReviews: 0,
      totalIssues: 0,
      repoCount: 0,
      avgIssuesPerReview: 0,
      severityDistribution: {
        CRITICAL: 0,
        HIGH: 0,
        MEDIUM: 0,
        LOW: 0,
        INFO: 0,
      },
      commonIssueTypes: [],
    })

    renderStatsPage()

    expect(await screen.findByText('暂无问题类型数据')).toBeInTheDocument()
  })
})
