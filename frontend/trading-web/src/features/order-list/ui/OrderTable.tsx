import type { OrderResponse, OrderStatus } from '../../../entities/order/model/types';

interface OrderTableProps {
  orders: OrderResponse[];
  onCancel: (orderId: number) => void;
  isCancelling: boolean;
}

const STATUS_BADGE_CLS: Record<OrderStatus, string> = {
  PENDING: 'bg-amber-600',
  PARTIAL: 'bg-blue-600',
  FILLED: 'bg-green-600',
  CANCELLED: 'bg-gray-500',
  REJECTED: 'bg-red-600',
};

const CANCEL_ACTIVE: Set<OrderStatus> = new Set(['PENDING', 'PARTIAL']);

function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`inline-block rounded px-2 py-0.5 text-[11px] font-bold text-white ${STATUS_BADGE_CLS[status]}`}>{status}</span>;
}

export function OrderTable({ orders, onCancel, isCancelling }: OrderTableProps) {
  if (orders.length === 0) {
    return <p className="rounded-xl border border-white/12 bg-bg-card px-4 py-6 text-center text-sm text-text-secondary">주문 내역이 없습니다.</p>;
  }

  return (
    <div className="overflow-x-auto rounded-[16px] border border-white/12 bg-black/20">
      <table className="min-w-full text-sm">
        <thead className="bg-white/[0.03] text-text-secondary">
          <tr className="[&>th]:px-3 [&>th]:py-2.5 [&>th]:text-left [&>th]:font-semibold [&>th]:whitespace-nowrap border-b border-white/12">
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
        <tbody className="[&>tr]:border-b [&>tr]:border-white/8 [&>tr:last-child]:border-b-0">
          {orders.map((order) => {
            const canCancel = CANCEL_ACTIVE.has(order.orderStatus);
            return (
              <tr key={order.orderId} className="hover:bg-white/[0.02]">
                <td className="whitespace-nowrap px-3 py-2.5 font-bold text-brand-primary">{order.orderId}</td>
                <td className="whitespace-nowrap px-3 py-2.5 font-semibold">{order.ticker}</td>
                <td className="whitespace-nowrap px-3 py-2.5">{order.marketType}</td>
                <td className="whitespace-nowrap px-3 py-2.5">
                  <span
                    className={`inline-block rounded px-2 py-0.5 text-[11px] font-bold text-white ${
                      order.orderSide === 'BUY' ? 'bg-blue-600' : 'bg-red-600'
                    }`}
                  >
                    {order.orderSide}
                  </span>
                </td>
                <td className="whitespace-nowrap px-3 py-2.5">{order.orderType}</td>
                <td className="whitespace-nowrap px-3 py-2.5">{order.orderCondition}</td>
                <td className="whitespace-nowrap px-3 py-2.5">{order.quantity} / {order.filledQuantity}</td>
                <td className="whitespace-nowrap px-3 py-2.5">{order.limitPrice ?? '-'}</td>
                <td className="whitespace-nowrap px-3 py-2.5">{order.avgFilledPrice ?? '-'}</td>
                <td className="whitespace-nowrap px-3 py-2.5">{order.fee}</td>
                <td className="whitespace-nowrap px-3 py-2.5"><StatusBadge status={order.orderStatus} /></td>
                <td className="whitespace-nowrap px-3 py-2.5 text-xs">
                  {new Date(order.createdAt).toLocaleString('ko-KR')}
                </td>
                <td className="whitespace-nowrap px-3 py-2.5">
                  <button
                    className="rounded-md border border-red-500/40 bg-red-500/10 px-2.5 py-1 text-xs font-semibold text-red-300 transition hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-45"
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
