import type { OrderResponse, OrderStatus } from '../../../entities/order/model/types';
import { Badge } from '../../../shared/ui/shadcn/badge';
import { Button } from '../../../shared/ui/shadcn/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../shared/ui/shadcn/table';

interface OrderTableProps {
  orders: OrderResponse[];
  onCancel: (orderId: number) => void;
  isCancelling: boolean;
}

const STATUS_BADGE_CLS: Record<OrderStatus, string> = {
  PENDING: 'border-market-warning/20 bg-market-warning/10 text-market-warning',
  PARTIAL: 'border-primary/20 bg-primary/10 text-primary',
  FILLED: 'border-market-positive/20 bg-market-positive/10 text-market-positive',
  CANCELLED: 'border-border bg-muted text-muted-foreground',
  REJECTED: 'border-destructive/20 bg-destructive/10 text-destructive',
};

const SIDE_BADGE_CLS = {
  BUY: 'border-order-buy/20 bg-order-buy/10 text-order-buy',
  SELL: 'border-order-sell/20 bg-order-sell/10 text-order-sell',
} as const;

const CANCEL_ACTIVE: Set<OrderStatus> = new Set(['PENDING', 'PARTIAL']);

function StatusBadge({ status }: { status: OrderStatus }) {
  return (
    <Badge variant="outline" className={STATUS_BADGE_CLS[status]} data-status={status}>
      {status}
    </Badge>
  );
}

export function OrderTable({ orders, onCancel, isCancelling }: OrderTableProps) {
  if (orders.length === 0) {
    return <p className="rounded-xl border bg-card px-4 py-6 text-center text-sm text-muted-foreground">주문 내역이 없습니다.</p>;
  }

  return (
    <div className="rounded-xl border bg-card">
      <Table>
        <TableHeader className="bg-muted/50">
          <TableRow>
            <TableHead>주문 ID</TableHead>
            <TableHead>종목</TableHead>
            <TableHead>시장</TableHead>
            <TableHead>매매</TableHead>
            <TableHead>유형</TableHead>
            <TableHead>조건</TableHead>
            <TableHead>수량/체결</TableHead>
            <TableHead>지정가</TableHead>
            <TableHead>평균체결가</TableHead>
            <TableHead>수수료</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>주문일시</TableHead>
            <TableHead>취소</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {orders.map((order) => {
            const canCancel = CANCEL_ACTIVE.has(order.orderStatus);
            return (
              <TableRow key={order.orderId}>
                <TableCell className="font-semibold text-primary">{order.orderId}</TableCell>
                <TableCell className="font-semibold">{order.ticker}</TableCell>
                <TableCell>{order.marketType}</TableCell>
                <TableCell>
                  <Badge
                    variant="outline"
                    className={SIDE_BADGE_CLS[order.orderSide]}
                    data-tone={order.orderSide === 'BUY' ? 'order-buy' : 'order-sell'}
                  >
                    {order.orderSide}
                  </Badge>
                </TableCell>
                <TableCell>{order.orderType}</TableCell>
                <TableCell>{order.orderCondition}</TableCell>
                <TableCell>{order.quantity} / {order.filledQuantity}</TableCell>
                <TableCell>{order.limitPrice ?? '-'}</TableCell>
                <TableCell>{order.avgFilledPrice ?? '-'}</TableCell>
                <TableCell>{order.fee}</TableCell>
                <TableCell><StatusBadge status={order.orderStatus} /></TableCell>
                <TableCell className="text-xs">
                  {new Date(order.createdAt).toLocaleString('ko-KR')}
                </TableCell>
                <TableCell>
                  <Button
                    type="button"
                    variant="destructive"
                    size="xs"
                    disabled={!canCancel || isCancelling}
                    onClick={() => onCancel(order.orderId)}
                  >
                    취소
                  </Button>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
