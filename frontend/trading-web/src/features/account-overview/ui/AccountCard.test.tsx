import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AccountCard } from './AccountCard'
import type { AccountResponse } from '../../../entities/account/model/types'

const baseAccount: AccountResponse = {
  id: 1,
  accountName: '테스트계좌',
  accountType: 'PAPER',
  tradingMode: 'LOCAL',
  deposit: '1000000.00',
  availableDeposit: '800000.00',
  lockedDeposit: '200000.00',
  baseCurrency: 'KRW',
  externalAccountId: null,
  isActive: true,
  createdAt: null,
  updatedAt: null,
}

describe('AccountCard', () => {
  it('renders accountName in the DOM', () => {
    render(<AccountCard account={baseAccount} isSelected={false} onClick={() => {}} />)
    expect(screen.getByText('테스트계좌')).toBeInTheDocument()
  })

  it('renders tradingMode badge label "LOCAL"', () => {
    render(<AccountCard account={baseAccount} isSelected={false} onClick={() => {}} />)
    expect(screen.getByText('LOCAL')).toBeInTheDocument()
  })

  it('shows inactive indicator when isActive=false', () => {
    const inactiveAccount: AccountResponse = { ...baseAccount, isActive: false }
    render(<AccountCard account={inactiveAccount} isSelected={false} onClick={() => {}} />)
    expect(screen.getByText('비활성')).toBeInTheDocument()
  })

  it('does not show inactive indicator when isActive=true', () => {
    render(<AccountCard account={baseAccount} isSelected={false} onClick={() => {}} />)
    expect(screen.queryByText('비활성')).not.toBeInTheDocument()
  })

  it('applies selected style when isSelected=true', () => {
    const { container } = render(
      <AccountCard account={baseAccount} isSelected={true} onClick={() => {}} />
    )
    const card = container.firstChild as HTMLElement
    // Selected card has a 2px solid border style
    expect(card.style.border).toContain('2px solid')
  })

  it('applies non-selected style when isSelected=false', () => {
    const { container } = render(
      <AccountCard account={baseAccount} isSelected={false} onClick={() => {}} />
    )
    const card = container.firstChild as HTMLElement
    expect(card.style.border).toContain('1px solid')
  })

  it('calls onClick when card is clicked', async () => {
    const user = userEvent.setup()
    const handleClick = vi.fn()
    render(<AccountCard account={baseAccount} isSelected={false} onClick={handleClick} />)
    await user.click(screen.getByText('테스트계좌'))
    expect(handleClick).toHaveBeenCalledTimes(1)
  })

  it('renders deposit value formatted', () => {
    render(<AccountCard account={baseAccount} isSelected={false} onClick={() => {}} />)
    // deposit "1000000.00" should appear with thousands separator
    const depositText = screen.getByText(/1,000,000/)
    expect(depositText).toBeInTheDocument()
  })

  it('renders availableDeposit value formatted', () => {
    render(<AccountCard account={baseAccount} isSelected={false} onClick={() => {}} />)
    const availText = screen.getByText(/800,000/)
    expect(availText).toBeInTheDocument()
  })
})
