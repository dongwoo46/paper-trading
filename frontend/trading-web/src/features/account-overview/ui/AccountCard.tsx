import type { AccountResponse } from "../../../entities/account/model/types";
import { formatAmount } from "../../../shared/utils/format";

const TRADING_MODE_LABELS: Record<string, string> = {
  LOCAL: "LOCAL",
  KIS_PAPER: "KIS 모의",
  KIS_LIVE: "KIS 실전",
};

const TRADING_MODE_COLORS: Record<string, string> = {
  LOCAL: "#6366f1",
  KIS_PAPER: "#f59e0b",
  KIS_LIVE: "#ef4444",
};

const ACCOUNT_TYPE_COLORS: Record<string, string> = {
  PAPER: "#10b981",
  LIVE: "#ef4444",
  VIRTUAL: "#8b5cf6",
};

interface AccountCardProps {
  account: AccountResponse;
  isSelected: boolean;
  onClick: () => void;
}

export function AccountCard({ account, isSelected, onClick }: AccountCardProps) {
  const modeColor = TRADING_MODE_COLORS[account.tradingMode] ?? "#6b7280";
  const typeColor = ACCOUNT_TYPE_COLORS[account.accountType] ?? "#6b7280";
  const modeLabel = TRADING_MODE_LABELS[account.tradingMode] ?? account.tradingMode;

  return (
    <div
      onClick={onClick}
      style={{
        cursor: "pointer",
        padding: "16px",
        borderRadius: "8px",
        border: isSelected ? "2px solid var(--accent, #6366f1)" : "1px solid var(--border, #e5e7eb)",
        backgroundColor: isSelected ? "var(--surface-selected, #f5f3ff)" : "var(--surface, #ffffff)",
        opacity: account.isActive ? 1 : 0.5,
        transition: "all 0.15s ease",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px", flexWrap: "wrap" }}>
        <span style={{ fontWeight: 600, fontSize: "14px" }}>{account.accountName}</span>
        <span
          style={{
            fontSize: "11px",
            padding: "2px 6px",
            borderRadius: "4px",
            backgroundColor: modeColor,
            color: "#fff",
            fontWeight: 500,
          }}
        >
          {modeLabel}
        </span>
        <span
          style={{
            fontSize: "11px",
            padding: "2px 6px",
            borderRadius: "4px",
            backgroundColor: typeColor,
            color: "#fff",
            fontWeight: 500,
          }}
        >
          {account.accountType}
        </span>
        {!account.isActive && (
          <span style={{ fontSize: "11px", color: "#9ca3af" }}>비활성</span>
        )}
      </div>
      <div style={{ fontSize: "13px", color: "var(--text-secondary, #6b7280)" }}>
        <div style={{ marginBottom: "4px" }}>
          예수금: <span style={{ color: "var(--text-primary, #111827)", fontWeight: 500 }}>
            {formatAmount(account.deposit, account.baseCurrency)}
          </span>
        </div>
        <div>
          가용: <span style={{ color: "var(--text-primary, #111827)", fontWeight: 500 }}>
            {formatAmount(account.availableDeposit, account.baseCurrency)}
          </span>
        </div>
      </div>
    </div>
  );
}
