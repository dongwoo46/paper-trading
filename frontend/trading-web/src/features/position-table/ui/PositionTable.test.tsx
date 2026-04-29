import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { PositionTable } from './PositionTable'
import type { PositionResponse } from '../../../entities/account/model/types'

const basePosition: PositionResponse = {
  ticker: 'AAPL',
  marketType: 'NASDAQ',
  quantity: '10',
  orderableQuantity: '10',
  lockedQuantity: '0',
  avgBuyPrice: '150.00',
  currentPrice: '160.00',
  evaluationAmount: '1600.00',
  unrealizedPnl: '100.00',
  returnRate: '6.67',
  priceSource: 'REDIS',
  priceUpdatedAt: null,
}

describe('PositionTable', () => {
  it('shows "포지션 없음" text when positions array is empty', () => {
    render(<PositionTable positions={[]} />)
    expect(screen.getByText('포지션 없음')).toBeInTheDocument()
  })

  it('renders ticker value in the table', () => {
    render(<PositionTable positions={[basePosition]} />)
    expect(screen.getByText('AAPL')).toBeInTheDocument()
  })

  it('shows "-" when currentPrice is null', () => {
    const nullPricePosition: PositionResponse = { ...basePosition, currentPrice: null }
    render(<PositionTable positions={[nullPricePosition]} />)
    // formatAmount(null) returns "-"
    // There will be multiple "-" (for evaluationAmount etc), just check at least one exists
    const dashes = screen.getAllByText('-')
    expect(dashes.length).toBeGreaterThan(0)
  })

  it('shows "+" prefix for positive unrealizedPnl', () => {
    render(<PositionTable positions={[basePosition]} />)
    // unrealizedPnl "100.00" is positive → "+100.00" formatted
    const pnlCell = screen.getByText(/^\+/)
    expect(pnlCell).toBeInTheDocument()
  })

  it('shows "-" for null unrealizedPnl', () => {
    const nullPnlPosition: PositionResponse = {
      ...basePosition,
      unrealizedPnl: null,
      returnRate: null,
      currentPrice: null,
      evaluationAmount: null,
    }
    render(<PositionTable positions={[nullPnlPosition]} />)
    const dashes = screen.getAllByText('-')
    expect(dashes.length).toBeGreaterThan(0)
  })

  it('applies green color for positive returnRate', () => {
    render(<PositionTable positions={[basePosition]} />)
    // returnRate "6.67" → positive → color #10b981
    const rateCell = screen.getByText('6.67%')
    expect(rateCell).toHaveStyle({ color: '#10b981' })
  })

  it('applies red color for negative returnRate', () => {
    const negPosition: PositionResponse = {
      ...basePosition,
      unrealizedPnl: '-50.00',
      returnRate: '-3.33',
    }
    render(<PositionTable positions={[negPosition]} />)
    const rateCell = screen.getByText('-3.33%')
    expect(rateCell).toHaveStyle({ color: '#ef4444' })
  })
})
