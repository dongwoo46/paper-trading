import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { Badge } from './badge'
import { Button } from './button'
import { Card, CardContent, CardHeader, CardTitle } from './card'
import { Input } from './input'
import { Label } from './label'
import { NativeSelect, NativeSelectOption } from './native-select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from './table'

describe('shadcn design-system primitives', () => {
  it('keeps button semantics and supports variants', () => {
    render(<Button variant="destructive">주문 취소</Button>)

    const button = screen.getByRole('button', { name: '주문 취소' })
    expect(button).toHaveAttribute('data-slot', 'button')
    expect(button).toHaveAttribute('data-variant', 'destructive')
  })

  it('composes a card through named slots', () => {
    render(
      <Card>
        <CardHeader>
          <CardTitle>계좌 요약</CardTitle>
        </CardHeader>
        <CardContent>가용 예수금</CardContent>
      </Card>,
    )

    expect(screen.getByText('계좌 요약')).toHaveAttribute('data-slot', 'card-title')
    expect(screen.getByText('가용 예수금')).toHaveAttribute('data-slot', 'card-content')
  })

  it('associates labels with form controls', () => {
    render(
      <div>
        <Label htmlFor="ticker">종목코드</Label>
        <Input id="ticker" />
      </div>,
    )

    expect(screen.getByRole('textbox', { name: '종목코드' })).toHaveAttribute('data-slot', 'input')
  })

  it('preserves native select behavior for trading forms', () => {
    render(
      <NativeSelect aria-label="시장">
        <NativeSelectOption value="KOSPI">KOSPI</NativeSelectOption>
        <NativeSelectOption value="NASDAQ">NASDAQ</NativeSelectOption>
      </NativeSelect>,
    )

    expect(screen.getByRole('combobox', { name: '시장' })).toHaveAttribute('data-slot', 'native-select')
    expect(screen.getAllByRole('option')).toHaveLength(2)
  })

  it('keeps semantic table roles for dense trading data', () => {
    render(
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>종목</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell>AAPL</TableCell>
          </TableRow>
        </TableBody>
      </Table>,
    )

    expect(screen.getByRole('table')).toHaveAttribute('data-slot', 'table')
    expect(screen.getByRole('columnheader', { name: '종목' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'AAPL' })).toBeInTheDocument()
  })

  it('renders badges as a reusable status primitive', () => {
    render(<Badge variant="secondary">PENDING</Badge>)

    expect(screen.getByText('PENDING')).toHaveAttribute('data-slot', 'badge')
  })
})
