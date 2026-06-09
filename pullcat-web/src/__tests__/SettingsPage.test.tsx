/// <reference types="@testing-library/jest-dom" />
import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { SettingsPage } from '../pages/SettingsPage'

vi.mock('../lib/api', () => ({
  getCurrentUser: vi.fn().mockResolvedValue({ authenticated: false }),
  getAutoPublishRepos: vi.fn().mockResolvedValue([]),
  setAutoPublish: vi.fn(),
  disableAutoPublish: vi.fn(),
}))

describe('SettingsPage', () => {
  it('removes the focus copy and keeps primary setup sections', async () => {
    render(
      <MemoryRouter>
        <SettingsPage />
      </MemoryRouter>,
    )

    expect(screen.queryByText('当前关注点')).not.toBeInTheDocument()
    expect(screen.queryByText('先把连接和自动化配好')).not.toBeInTheDocument()
    expect(screen.queryByText('连接与自动化')).not.toBeInTheDocument()
    expect(screen.queryByText('先确认 GitHub 连接正常，再为需要的仓库补上 Webhook 和自动发布')).not.toBeInTheDocument()
    expect(screen.queryByText('设置')).not.toBeInTheDocument()

    expect(await screen.findByText('登录状态')).toBeInTheDocument()
    expect(screen.getByText('登录状态')).toBeInTheDocument()
    expect(screen.getByText('Webhook 仓库')).toBeInTheDocument()
    expect(screen.getByText('自动发布')).toBeInTheDocument()
  })
})
