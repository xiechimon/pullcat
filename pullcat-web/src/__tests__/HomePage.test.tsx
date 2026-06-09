/// <reference types="@testing-library/jest-dom" />
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { toast } from 'sonner'
import { HomePage } from '../pages/HomePage'

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
  })

  it('renders an input-first review workspace', () => {
    renderHomePage()
    expect(screen.getByRole('heading', { name: '开始一次新审查' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText(/github\.com.*pull/)).toBeInTheDocument()
    expect(screen.getByText('支持公开 GitHub Pull Request 链接，提交后会立即创建审查会话')).toBeInTheDocument()
  })

  it('renders review steps and submit button', () => {
    renderHomePage()
    expect(screen.getByText('粘贴 PR 链接')).toBeInTheDocument()
    expect(screen.getByText('等待 AI 分析')).toBeInTheDocument()
    expect(screen.getByText('确认并发布评论')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /审查|review/i })).toBeInTheDocument()
  })

  it('validates invalid URL', () => {
    const toastSpy = vi.spyOn(toast, 'error')
    renderHomePage()
    const input = screen.getByPlaceholderText(/github\.com.*pull/)
    fireEvent.change(input, { target: { value: 'not-a-valid-url' } })
    fireEvent.click(screen.getByRole('button', { name: /审查|review/i }))
    expect(toastSpy).toHaveBeenCalledWith(expect.stringMatching(/无效|invalid|请输入/i))
  })
})
