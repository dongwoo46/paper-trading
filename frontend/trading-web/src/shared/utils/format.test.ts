import { describe, it, expect } from 'vitest'
import { formatAmount, formatRate } from './format'

describe('formatAmount', () => {
  it('returns "-" for null', () => {
    expect(formatAmount(null)).toBe('-')
  })

  it('formats "10000.5" with thousands separator containing "10,000"', () => {
    const result = formatAmount('10000.5')
    expect(result).toContain('10,000')
  })

  it('does not crash for "0.00"', () => {
    const result = formatAmount('0.00')
    expect(typeof result).toBe('string')
    expect(result).not.toBe('-')
  })
})

describe('formatRate', () => {
  it('returns "-" for null', () => {
    expect(formatRate(null)).toBe('-')
  })

  it('formats "12.34" as "12.34%"', () => {
    expect(formatRate('12.34')).toBe('12.34%')
  })

  it('formats "-5.67" as "-5.67%"', () => {
    expect(formatRate('-5.67')).toBe('-5.67%')
  })
})
