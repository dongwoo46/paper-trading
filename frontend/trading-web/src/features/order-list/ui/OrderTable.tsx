import type { OrderResponse, OrderStatus } from '../../../entities/order/model/types';

interface OrderTableProps {
  orders: OrderResponse[];
  onCancel: (orderId: number) => void;
  isCancelling: boolean;
}

const STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING: '#d97706',
  PARTIAL: '#2563eb',
  FILLED: '#16a34a',
  CANCELLED: '#6b7280',
  REJECTED: '#dc2626',
};

const CANCEL_ACTIVE: Set<OrderStatus> = new Set(['PENDING', 'PARTIAL']);

function StatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 8px',
        borderRadius: '4px',
        fontSize: '11px',
        fontWeight: 700,
        color: '#fff',
        background: STATUS_COLORS[status],
      }}
    >
      {status}
    </span>
  );
}

export function OrderTable({ orders, onCancel, isCancelling }: OrderTableProps) {
  if (orders.length === 0) {
    return <p className="empty-state">주문 내역이 없습니다.</p>;
  }

  return (
    <div className="table-container" style={{ overflowX: 'auto' }}>
      <table>
        <thead>
          <tr>
            <th>주문 ID</th>
            <th>종목</th>
            <th>시장</th>
            <th>매매</th>
            <th>유형</th>
            <th>조건</th>
            <th>수량/체결</th>
            <th>지정가</th>
            <th>평균체결가</th>
            <th>수수료</th>
            <th>상태</th>
            <th>주문일시</th>
            <th>취소</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => {
            const canCancel = CANCEL_ACTIVE.has(order.orderStatus);
            return (
              <tr key={order.orderId}>
                <td style={{ fontWeight: 700, color: 'var(--brand-primary)' }}>{order.orderId}</td>
                <td style={{ fontWeight: 600 }}>{order.ticker}</td>
                <td>{order.marketType}</td>
                <td>
                  <span
                    style={{
                      display: 'inline-block',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontSize: '11px',
                      fontWeight: 700,
                      color: '#fff',
                      background: order.orderSide === 'BUY' ? '#2563eb' : '#dc2626',
                    }}
                  >
                    {order.orderSide}
                  </span>
                </td>
                <td>{order.orderType}</td>
                <td>{order.orderCondition}</td>
                <td>{order.quantity} / {order.filledQuantity}</td>
                <td>{order.limitPrice ?? '-'}</td>
                <td>{order.avgFilledPrice ?? '-'}</td>
                <td>{order.fee}</td>
                <td><StatusBadge status={order.orderStatus} /></td>
                <td style={{ fontSize: '12px', whiteSpace: 'nowrap' }}>
                  {new Date(order.createdAt).toLocaleString('ko-KR')}
                </td>
                <td>
                  <button
                    className="btn btn-danger"
                    style={{ padding: '4px 10px', fontSize: '12px' }}
                    disabled={!canCancel || isCancelling}
                    onClick={() => onCancel(order.orderId)}
                  >
                    취소
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
