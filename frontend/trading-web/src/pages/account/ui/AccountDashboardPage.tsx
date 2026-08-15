import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";
import { fetchAccounts, fetchPositions } from "../../../entities/account/api/accountApi";
import { AccountCard } from "../../../features/account-overview/ui/AccountCard";
import { PositionTable } from "../../../features/position-table/ui/PositionTable";
import { PageHeader } from "../../../shared/ui/PageHeader";
import { Alert, AlertDescription } from "../../../shared/ui/shadcn/alert";
import { Button } from "../../../shared/ui/shadcn/button";
import { Skeleton } from "../../../shared/ui/shadcn/skeleton";

export function AccountDashboardPage() {
  const queryClient = useQueryClient();

  const {
    data: accounts = [],
    isLoading: isAccountsLoading,
    isError: isAccountsError,
  } = useQuery({
    queryKey: ["accounts", "list"],
    queryFn: fetchAccounts,
    staleTime: 30_000,
  });

  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);

  const resolvedAccountId =
    selectedAccountId ?? (accounts.length > 0 ? accounts[0].id : null);

  const {
    data: positions = [],
    isLoading: isPositionsLoading,
    isError: isPositionsError,
  } = useQuery({
    queryKey: ["positions", resolvedAccountId],
    queryFn: () => fetchPositions(resolvedAccountId!),
    enabled: resolvedAccountId !== null,
    staleTime: 0,
  });

  const selectedAccount = accounts.find((a) => a.id === resolvedAccountId) ?? null;

  const handleRefresh = () => {
    void queryClient.invalidateQueries({ queryKey: ["positions", resolvedAccountId] });
  };

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <PageHeader
        eyebrow="개요"
        title="계좌·포지션"
        description={'계좌별 예수금과 보유 포지션을 확인합니다. 현재가는 Redis 시세 기준이며 장 외 시간에는 "-"로 표시될 수 있습니다.'}
      />

      {/* Account List Section */}
      <div className="mb-6">
        <h3 className="mb-3 text-sm font-semibold text-muted-foreground">계좌 목록</h3>
        {isAccountsLoading && (
          <Skeleton className="h-24 w-full p-6 text-center text-muted-foreground">
            계좌 정보를 불러오는 중...
          </Skeleton>
        )}
        {isAccountsError && (
          <Alert variant="destructive">
            <AlertDescription>계좌 목록을 불러오지 못했습니다.</AlertDescription>
          </Alert>
        )}
        {!isAccountsLoading && !isAccountsError && accounts.length === 0 && (
          <Alert>
            <AlertDescription className="text-center">등록된 계좌가 없습니다.</AlertDescription>
          </Alert>
        )}
        {!isAccountsLoading && !isAccountsError && accounts.length > 0 && (
          <div className="grid grid-cols-[repeat(auto-fill,minmax(240px,1fr))] gap-3">
            {accounts.map((account) => (
              <AccountCard
                key={account.id}
                account={account}
                isSelected={account.id === resolvedAccountId}
                onClick={() => setSelectedAccountId(account.id)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Position Section */}
      {resolvedAccountId !== null && (
        <div>
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-muted-foreground">
              포지션 — {selectedAccount?.accountName ?? ""}
            </h3>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleRefresh}
            >
              <RefreshCw data-icon="inline-start" />
              새로고침
            </Button>
          </div>

          {isPositionsLoading && (
            <Skeleton className="h-24 w-full p-6 text-center text-muted-foreground">
              포지션을 불러오는 중...
            </Skeleton>
          )}
          {isPositionsError && (
            <Alert variant="destructive">
              <AlertDescription>포지션 정보를 불러오지 못했습니다.</AlertDescription>
            </Alert>
          )}
          {!isPositionsLoading && !isPositionsError && (
            <div className="overflow-hidden rounded-xl border bg-card">
              <PositionTable positions={positions} />
            </div>
          )}
        </div>
      )}
    </section>
  );
}
