/// <reference types="@testing-library/jest-dom" />
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { HistoryPage } from '../pages/HistoryPage'

vi.mock('../lib/api', () => ({
  getReviews: vi.fn(),
  deleteReview: vi.fn(),
}))

import { deleteReview, getReviews } from '../lib/api'

function renderHistory(initialEntries = ['/history']) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <Routes>
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/compare" element={<div>compare target</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('HistoryPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders empty state call to action', async () => {
    vi.mocked(getReviews).mockResolvedValue({
      items: [],
      total: 0,
      page: 0,
      size: 20,
    })

    renderHistory()

    expect(await screen.findByPlaceholderText('筛选仓库，例如 owner/repo')).toBeInTheDocument()
    expect(screen.getByText('暂无审查记录')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '开始审查' })).toBeInTheDocument()
  })

  it('renders review rows and compare controls', async () => {
    vi.mocked(getReviews).mockResolvedValue({
      items: [
        {
          id: 'review-1',
          prUrl: 'https://github.com/pullcat/pullcat/pull/1',
          status: 'COMPLETED',
          prMetadata: {
            title: 'Refine history page',
            description: '',
            owner: 'pullcat',
            repo: 'pullcat',
            pullNumber: 1,
            baseBranch: 'main',
            headBranch: 'feat/history',
            fileCount: 2,
            additions: 20,
            deletions: 4,
          },
          analyses: {
            summary: {
              type: 'summary',
              status: 'COMPLETED',
              model: 'gpt',
              content: '',
              issues: [],
              tokensUsed: 0,
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
        {
          id: 'review-2',
          prUrl: 'https://github.com/pullcat/pullcat/pull/2',
          status: 'ANALYZING',
          prMetadata: {
            title: 'Compact dashboard copy',
            description: '',
            owner: 'pullcat',
            repo: 'pullcat',
            pullNumber: 2,
            baseBranch: 'main',
            headBranch: 'feat/dashboard-copy',
            fileCount: 1,
            additions: 10,
            deletions: 1,
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
                  file: 'src/app.tsx',
                  line: 1,
                  title: 'One issue',
                  description: '',
                  suggestion: '',
                  confidence: 0.8,
                  selected: true,
                },
              ],
              tokensUsed: 0,
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
      total: 2,
      page: 0,
      size: 20,
    })

    renderHistory()

    expect(await screen.findByText('Refine history page')).toBeInTheDocument()
    expect(screen.getByText('Compact dashboard copy')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '选择对比' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '选择对比' }))

    expect(screen.getByText('已选 0/2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '开始对比' })).toBeDisabled()
  })

  it('applies repo filter on enter', async () => {
    vi.mocked(getReviews).mockResolvedValue({
      items: [],
      total: 0,
      page: 0,
      size: 20,
    })

    renderHistory()

    const input = await screen.findByPlaceholderText('筛选仓库，例如 owner/repo')
    fireEvent.change(input, { target: { value: 'pullcat/pullcat' } })
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' })

    expect(vi.mocked(getReviews)).toHaveBeenLastCalledWith(0, 20, 'pullcat/pullcat')
  })

  it('deletes a review after confirmation', async () => {
    vi.mocked(getReviews).mockResolvedValue({
      items: [
        {
          id: 'review-1',
          prUrl: 'https://github.com/pullcat/pullcat/pull/1',
          status: 'COMPLETED',
          prMetadata: {
            title: 'Delete target',
            description: '',
            owner: 'pullcat',
            repo: 'pullcat',
            pullNumber: 1,
            baseBranch: 'main',
            headBranch: 'feat/history',
            fileCount: 2,
            additions: 20,
            deletions: 4,
          },
          analyses: {},
          createdAt: '2026-06-09T06:00:00.000Z',
          completedAt: null,
          publishedCommentId: null,
          repositoryFullName: 'pullcat/pullcat',
          rawDiff: null,
        },
      ],
      total: 1,
      page: 0,
      size: 20,
    })
    vi.mocked(deleteReview).mockResolvedValue({ deleted: true })

    renderHistory()

    fireEvent.click(await screen.findByRole('button', { name: '删除审查记录' }))
    fireEvent.click(screen.getByRole('button', { name: '删除' }))

    expect(vi.mocked(deleteReview)).toHaveBeenCalledWith('review-1')
  })
})
