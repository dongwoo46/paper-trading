import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";
import { fetchAccounts, fetchPositions } from "../../../entities/account/api/accountApi";
import { AccountCard } from "../../../features/account-overview/ui/AccountCard";
import { PositionTable } from "../../../features/position-table/ui/PositionTable";

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
      <div className="flex flex-col gap-1.5">
        <h2 className="text-[28px] font-bold tracking-tight">계좌·포지션</h2>
        <p className="text-text-secondary text-[15px] max-w-3xl">
          계좌별 예수금 현황과 보유 포지션을 확인합니다. 포지션의 현재가는 Redis 시세 기준이며, 장 외 시간에는 "-"로 표시될 수 있습니다.
        </p>
      </div>

      {/* Account List Section */}
      <div className="mb-6">
        <h3 className="text-sm font-semibold mb-3 text-text-muted">계좌 목록</h3>
        {isAccountsLoading && (
          <div className="py-6 text-center text-text-muted">계좌 정보를 불러오는 중...</div>
        )}
        {isAccountsError && (
          <div className="p-4 text-status-error bg-red-500/8 border border-red-500/20 rounded-xl">
            계좌 목록을 불러오지 못했습니다.
          </div>
        )}
        {!isAccountsLoading && !isAccountsError && accounts.length === 0 && (
          <div className="py-6 text-center text-text-muted">등록된 계좌가 없습니다.</div>
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
            <h3 className="text-sm font-semibold text-text-muted">
              포지션 — {selectedAccount?.accountName ?? ""}
            </h3>
            <button
              onClick={handleRefresh}
              className="flex items-center gap-1.5 px-3 py-1.5 text-[13px] border border-white/12 rounded-lg bg-transparent text-text-primary hover:bg-white/5 transition-all cursor-pointer"
            >
              <RefreshCw size={14} />
              새로고침
            </button>
          </div>

          {isPositionsLoading && (
            <div className="py-6 text-center text-text-muted">포지션을 불러오는 중...</div>
          )}
          {isPositionsError && (
            <div className="p-4 text-status-error bg-red-500/8 border border-red-500/20 rounded-xl">
              포지션 정보를 불러오지 못했습니다.
            </div>
          )}
          {!isPositionsLoading && !isPositionsError && (
            <div className="overflow-x-auto">
              <PositionTable positions={positions} />
            </div>
          )}
        </div>
      )}
    </section>
  );
}
