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
    <section className="panel">
      <div className="panel-header">
        <h2>계좌·포지션</h2>
        <p className="lead">
          계좌별 예수금 현황과 보유 포지션을 확인합니다. 포지션의 현재가는 Redis 시세 기준이며, 장 외 시간에는 "-"로 표시될 수 있습니다.
        </p>
      </div>

      {/* Account List Section */}
      <div style={{ marginBottom: "24px" }}>
        <h3 style={{ fontSize: "14px", fontWeight: 600, marginBottom: "12px", color: "var(--text-secondary, #6b7280)" }}>
          계좌 목록
        </h3>
        {isAccountsLoading && (
          <div style={{ padding: "24px", textAlign: "center", color: "var(--text-secondary, #6b7280)" }}>
            계좌 정보를 불러오는 중...
          </div>
        )}
        {isAccountsError && (
          <div style={{ padding: "16px", color: "#ef4444", backgroundColor: "#fef2f2", borderRadius: "6px" }}>
            계좌 목록을 불러오지 못했습니다.
          </div>
        )}
        {!isAccountsLoading && !isAccountsError && accounts.length === 0 && (
          <div style={{ padding: "24px", textAlign: "center", color: "var(--text-secondary, #6b7280)" }}>
            등록된 계좌가 없습니다.
          </div>
        )}
        {!isAccountsLoading && !isAccountsError && accounts.length > 0 && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))", gap: "12px" }}>
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
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "12px" }}>
            <h3 style={{ fontSize: "14px", fontWeight: 600, color: "var(--text-secondary, #6b7280)" }}>
              포지션 — {selectedAccount?.accountName ?? ""}
            </h3>
            <button
              onClick={handleRefresh}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "6px",
                padding: "6px 12px",
                fontSize: "13px",
                border: "1px solid var(--border, #e5e7eb)",
                borderRadius: "6px",
                background: "transparent",
                cursor: "pointer",
                color: "var(--text-primary, #111827)",
              }}
            >
              <RefreshCw size={14} />
              새로고침
            </button>
          </div>

          {isPositionsLoading && (
            <div style={{ padding: "24px", textAlign: "center", color: "var(--text-secondary, #6b7280)" }}>
              포지션을 불러오는 중...
            </div>
          )}
          {isPositionsError && (
            <div style={{ padding: "16px", color: "#ef4444", backgroundColor: "#fef2f2", borderRadius: "6px" }}>
              포지션 정보를 불러오지 못했습니다.
            </div>
          )}
          {!isPositionsLoading && !isPositionsError && (
            <div style={{ overflowX: "auto" }}>
              <PositionTable positions={positions} />
            </div>
          )}
        </div>
      )}
    </section>
  );
}
