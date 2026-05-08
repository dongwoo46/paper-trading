import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchCancelOrder, fetchListOrders } from '../../../entities/order/api/orderApi';
import type { OrderStatus } from '../../../entities/order/model/types';
import { OrderTable } from './OrderTable';

interface OrderTablePanelProps {
  accountId: number;
}

type FilterStatus = 'ALL' | OrderStatus;

const FILTER_OPTIONS: FilterStatus[] = ['ALL', 'PENDING', 'PARTIAL', 'FILLED', 'CANCELLED', 'REJECTED'];

export function OrderTablePanel({ accountId }: OrderTablePanelProps) {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<FilterStatus>('ALL');

  const { data: orders = [], isLoading, isError } = useQuery({
    queryKey: ['orders', accountId],
    queryFn: () => fetchListOrders(accountId),
    refetchInterval: 5000,
  });

  const cancelMutation = useMutation({
    mutationFn: (orderId: number) => fetchCancelOrder(accountId, orderId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['orders', accountId] });
    },
  });

  const handleCancel = (orderId: number) => {
    if (!window.confirm(`주문 #${orderId}을(를) 취소하시겠습니까?`)) return;
    cancelMutation.mutate(orderId);
  };

  const filteredOrders = filter === 'ALL'
    ? orders
    : orders.filter((o) => o.orderStatus === filter);

  return (
    <div className="mt-3 flex flex-col gap-3">
      <div className="inline-flex flex-wrap gap-2 rounded-[16px] border border-white/12 bg-white/[0.03] p-1">
        {FILTER_OPTIONS.map((status) => (
          <button
            key={status}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border ${
              filter === status
                ? 'bg-bg-card text-brand-primary border-white/12'
                : 'border-transparent text-text-secondary hover:text-text-primary'
            }`}
            onClick={() => setFilter(status)}
          >
            {status === 'ALL' ? '전체' : status}
          </button>
        ))}
      </div>

      {isLoading && <p className="text-sm text-text-secondary">주문 목록 로딩 중...</p>}
      {isError && <p className="text-sm text-status-error">주문 목록 조회 실패</p>}
      {cancelMutation.isError && (
        <p className="text-[13px] text-status-error">
          취소 실패: {cancelMutation.error instanceof Error ? cancelMutation.error.message : '알 수 없는 오류'}
        </p>
      )}

      {!isLoading && !isError && (
        <OrderTable
          orders={filteredOrders}
          onCancel={handleCancel}
          isCancelling={cancelMutation.isPending}
        />
      )}
    </div>
  );
}
