/// <reference types="@testing-library/jest-dom" />
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { toast } from 'sonner'
import { HomePage } from '../pages/HomePage'

vi.mock('../lib/api', () => ({
  createReview: vi.fn(),
  getReviews: vi.fn(),
}))

import { getReviews } from '../lib/api'

function renderHomePage() {
  return render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>
  )
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.mocked(getReviews).mockResolvedValue({
      items: [],
      total: 0,
      page: 0,
      size: 10,
    })
  })

  it('renders an input-first review workspace', () => {
    renderHomePage()
    expect(screen.getByPlaceholderText('输入 GitHub Pull Request 链接')).toBeInTheDocument()
  })

  it('renders submit button and recent reviews section', async () => {
    renderHomePage()
    expect(screen.getByRole('button', { name: '开始审查' })).toBeInTheDocument()
    expect(await screen.findByText('最近审查')).toBeInTheDocument()
  })

  it('validates invalid URL', () => {
    const toastSpy = vi.spyOn(toast, 'error')
    renderHomePage()
    const input = screen.getByPlaceholderText('输入 GitHub Pull Request 链接')
    fireEvent.change(input, { target: { value: 'not-a-valid-url' } })
    fireEvent.click(screen.getByRole('button', { name: /审查|review/i }))
    expect(toastSpy).toHaveBeenCalledWith(expect.stringMatching(/无效|invalid|请输入/i))
  })
})
