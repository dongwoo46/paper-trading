import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchCancelOrder, fetchListOrders } from '../../../entities/order/api/orderApi';
import type { OrderStatus } from '../../../entities/order/model/types';
import { Alert, AlertDescription } from '../../../shared/ui/shadcn/alert';
import { Button } from '../../../shared/ui/shadcn/button';
import { Skeleton } from '../../../shared/ui/shadcn/skeleton';
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
      <div className="inline-flex w-fit flex-wrap gap-1 rounded-xl border bg-card p-1" role="group" aria-label="주문 상태 필터">
        {FILTER_OPTIONS.map((status) => (
          <Button
            key={status}
            type="button"
            size="sm"
            variant={filter === status ? 'secondary' : 'ghost'}
            aria-pressed={filter === status}
            onClick={() => setFilter(status)}
          >
            {status === 'ALL' ? '전체' : status}
          </Button>
        ))}
      </div>

      {isLoading && <Skeleton className="h-24 w-full p-4 text-sm text-muted-foreground">주문 목록 로딩 중...</Skeleton>}
      {isError && (
        <Alert variant="destructive">
          <AlertDescription>주문 목록 조회 실패</AlertDescription>
        </Alert>
      )}
      {cancelMutation.isError && (
        <Alert variant="destructive">
          <AlertDescription>
            취소 실패: {cancelMutation.error instanceof Error ? cancelMutation.error.message : '알 수 없는 오류'}
          </AlertDescription>
        </Alert>
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
