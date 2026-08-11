import type { AccountResponse } from "../../../entities/account/model/types";
import { cn } from "../../../shared/lib/utils";
import { Badge } from "../../../shared/ui/shadcn/badge";
import { Button } from "../../../shared/ui/shadcn/button";
import { formatAmount } from "../../../shared/utils/format";

const TRADING_MODE_LABELS: Record<string, string> = {
  LOCAL: "LOCAL",
  KIS_PAPER: "KIS 모의",
  KIS_LIVE: "KIS 실전",
};

const TRADING_MODE_STYLES: Record<string, string> = {
  LOCAL: "border-primary/20 bg-primary/10 text-primary",
  KIS_PAPER: "border-market-warning/20 bg-market-warning/10 text-market-warning",
  KIS_LIVE: "border-destructive/20 bg-destructive/10 text-destructive",
};

const ACCOUNT_TYPE_STYLES: Record<string, string> = {
  PAPER: "border-market-positive/20 bg-market-positive/10 text-market-positive",
  LIVE: "border-destructive/20 bg-destructive/10 text-destructive",
  VIRTUAL: "border-primary/20 bg-secondary text-secondary-foreground",
};

interface AccountCardProps {
  account: AccountResponse;
  isSelected: boolean;
  onClick: () => void;
}

export function AccountCard({ account, isSelected, onClick }: AccountCardProps) {
  const modeStyle = TRADING_MODE_STYLES[account.tradingMode] ?? "border-border bg-muted text-muted-foreground";
  const typeStyle = ACCOUNT_TYPE_STYLES[account.accountType] ?? "border-border bg-muted text-muted-foreground";
  const modeLabel = TRADING_MODE_LABELS[account.tradingMode] ?? account.tradingMode;

  return (
    <Button
      type="button"
      variant="outline"
      onClick={onClick}
      aria-pressed={isSelected}
      aria-disabled={!account.isActive}
      data-selected={isSelected}
      data-active={account.isActive}
      className={cn(
        "h-auto w-full flex-col items-stretch justify-start gap-3 whitespace-normal p-4 text-left",
        isSelected
          ? "border-primary bg-accent/30 ring-2 ring-primary/20"
          : "border-border bg-card hover:bg-muted/50",
        !account.isActive && "opacity-50",
      )}
    >
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm font-semibold">{account.accountName}</span>
        <Badge variant="outline" className={modeStyle}>
          {modeLabel}
        </Badge>
        <Badge variant="outline" className={typeStyle}>
          {account.accountType}
        </Badge>
        {!account.isActive && (
          <span className="text-xs text-muted-foreground">비활성</span>
        )}
      </div>
      <div className="space-y-1 text-sm text-muted-foreground">
        <div>
          예수금: <span className="font-medium text-foreground">
            {formatAmount(account.deposit, account.baseCurrency)}
          </span>
        </div>
        <div>
          가용: <span className="font-medium text-foreground">
            {formatAmount(account.availableDeposit, account.baseCurrency)}
          </span>
        </div>
      </div>
    </Button>
  );
}
