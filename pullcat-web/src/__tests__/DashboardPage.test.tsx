/// <reference types="@testing-library/jest-dom" />
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { DashboardPage } from '../pages/DashboardPage'

vi.mock('../lib/api', () => ({
  getStatsOverview: vi.fn(),
  getReviews: vi.fn(),
}))

import { getReviews, getStatsOverview } from '../lib/api'

function renderDashboard() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders empty-state call to action when there are no reviews', async () => {
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
    vi.mocked(getReviews).mockResolvedValue({
      items: [],
      total: 0,
      page: 0,
      size: 10,
    })

    renderDashboard()

    expect(await screen.findByText('还没有审查记录')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '开始第一次审查' })).toBeInTheDocument()
  })

  it('renders inline error state when loading fails', async () => {
    vi.mocked(getStatsOverview).mockRejectedValue(new Error('network down'))
    vi.mocked(getReviews).mockResolvedValue({
      items: [],
      total: 0,
      page: 0,
      size: 10,
    })

    renderDashboard()

    expect(await screen.findByText('仪表盘暂时不可用')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '重新加载' })).toBeInTheDocument()
  })

  it('renders recent review details with repository and status', async () => {
    vi.mocked(getStatsOverview).mockResolvedValue({
      totalReviews: 12,
      totalIssues: 18,
      repoCount: 4,
      avgIssuesPerReview: 1.5,
      severityDistribution: {
        CRITICAL: 1,
        HIGH: 2,
        MEDIUM: 4,
        LOW: 6,
        INFO: 5,
      },
      commonIssueTypes: [],
    })
    vi.mocked(getReviews).mockResolvedValue({
      items: [
        {
          id: 'review-1',
          prUrl: 'https://github.com/pullcat/pullcat/pull/1',
          status: 'COMPLETED',
          prMetadata: {
            title: 'Refine dashboard experience',
            description: '',
            owner: 'pullcat',
            repo: 'pullcat',
            pullNumber: 1,
            baseBranch: 'main',
            headBranch: 'feat/dashboard',
            fileCount: 3,
            additions: 20,
            deletions: 5,
          },
          analyses: {
            summary: {
              type: 'summary',
              status: 'COMPLETED',
              model: 'gpt',
              content: '',
              issues: [
                {
                  id: 'issue-1',
                  severity: 'HIGH',
                  file: 'src/dashboard.tsx',
                  line: 12,
                  title: 'Missing retry state',
                  description: '',
                  suggestion: '',
                  confidence: 0.8,
                  selected: true,
                },
              ],
              tokensUsed: 10,
              startedAt: null,
              completedAt: null,
              errorMessage: null,
            },
          },
          createdAt: '2026-06-09T06:00:00.000Z',
          completedAt: null,
          publishedCommentId: null,
          repositoryFullName: 'pullcat/pullcat',
          rawDiff: null,
        },
      ],
      total: 1,
      page: 0,
      size: 10,
    })

    renderDashboard()

    expect((await screen.findAllByText('Refine dashboard experience')).length).toBeGreaterThan(0)
    expect(screen.getByText('工作摘要')).toBeInTheDocument()
    expect(screen.getAllByText('高严重度').length).toBeGreaterThan(0)
    expect(screen.getAllByText('pullcat/pullcat').length).toBeGreaterThan(0)
    expect(screen.getAllByText('1 个问题').length).toBeGreaterThan(0)
    expect(screen.getAllByText('已完成').length).toBeGreaterThan(0)
  })
})
