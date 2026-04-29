import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AccountDashboardPage } from './AccountDashboardPage'
import type { AccountResponse, PositionResponse } from '../../../entities/account/model/types'

// Mock the accountApi module
vi.mock('../../../entities/account/api/accountApi', () => ({
  fetchAccounts: vi.fn(),
  fetchPositions: vi.fn(),
}))

import { fetchAccounts, fetchPositions } from '../../../entities/account/api/accountApi'

const mockFetchAccounts = vi.mocked(fetchAccounts)
const mockFetchPositions = vi.mocked(fetchPositions)

const testAccount: AccountResponse = {
  id: 1,
  accountName: '통합테스트계좌',
  accountType: 'PAPER',
  tradingMode: 'LOCAL',
  deposit: '5000000.00',
  availableDeposit: '4000000.00',
  lockedDeposit: '1000000.00',
  baseCurrency: 'KRW',
  externalAccountId: null,
  isActive: true,
  createdAt: null,
  updatedAt: null,
}

const emptyPositions: PositionResponse[] = []

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

describe('AccountDashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Default positions mock to return empty array
    mockFetchPositions.mockResolvedValue(emptyPositions)
  })

  it('shows loading indicator while accounts are loading', async () => {
    // fetchAccounts never resolves during this test
    mockFetchAccounts.mockImplementation(
      () => new Promise<AccountResponse[]>(() => {})
    )

    render(<AccountDashboardPage />, { wrapper: createWrapper() })

    expect(screen.getByText('계좌 정보를 불러오는 중...')).toBeInTheDocument()
  })

  it('renders AccountCard after accounts load successfully', async () => {
    mockFetchAccounts.mockResolvedValue([testAccount])

    render(<AccountDashboardPage />, { wrapper: createWrapper() })

    await waitFor(() => {
      expect(screen.getByText('통합테스트계좌')).toBeInTheDocument()
    })
  })

  it('shows empty state message when fetchAccounts returns empty array', async () => {
    mockFetchAccounts.mockResolvedValue([])

    render(<AccountDashboardPage />, { wrapper: createWrapper() })

    await waitFor(() => {
      expect(screen.getByText('등록된 계좌가 없습니다.')).toBeInTheDocument()
    })
  })

  it('shows error message when fetchAccounts throws', async () => {
    mockFetchAccounts.mockRejectedValue(new Error('Network error'))

    render(<AccountDashboardPage />, { wrapper: createWrapper() })

    await waitFor(() => {
      expect(screen.getByText('계좌 목록을 불러오지 못했습니다.')).toBeInTheDocument()
    })
  })
})
