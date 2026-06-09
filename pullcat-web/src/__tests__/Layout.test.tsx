/// <reference types="@testing-library/jest-dom" />
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Layout } from '../components/Layout'

vi.mock('../lib/api', () => ({
  getCurrentUser: vi.fn().mockResolvedValue({ authenticated: false }),
  logout: vi.fn(),
}))

vi.mock('../components/ThemeToggle', () => ({
  ThemeToggle: () => <button type="button">切换主题</button>,
}))

function renderLayout(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Layout>
        <div>page body</div>
      </Layout>
    </MemoryRouter>,
  )
}

describe('Layout', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('shows desktop navigation entries', async () => {
    renderLayout('/history')

    expect(await screen.findByRole('link', { name: '仪表盘' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '历史' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '统计' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '新建审查' })).toBeInTheDocument()
  })

  it('shows mobile navigation trigger and opens mobile menu', async () => {
    renderLayout('/')

    const trigger = await screen.findByRole('button', { name: '打开导航菜单' })
    expect(trigger).toHaveAttribute('aria-expanded', 'false')

    fireEvent.click(trigger)

    await waitFor(() => {
      expect(screen.getByRole('dialog', { name: '移动端导航' })).toBeInTheDocument()
    })
    expect(screen.getAllByRole('link', { name: '新建审查' }).length).toBeGreaterThan(0)
    expect(trigger).toHaveAttribute('aria-expanded', 'true')
  })
})
