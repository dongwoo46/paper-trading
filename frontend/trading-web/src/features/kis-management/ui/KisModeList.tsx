import { Chip } from "../../../shared/ui";
import type { ModeSubscriptions } from "../../../shared/api";
import type { Mode } from "../../../entities/symbol/model/types";

interface Props {
  data: ModeSubscriptions;
  symbolNameMap: Record<string, string>;
}

export function KisModeList({ data, symbolNameMap }: Props) {
  const paperList = Array.isArray(data?.paper) ? data.paper : [];
  const liveList = Array.isArray(data?.live) ? data.live : [];

  const allSymbols = [
    ...paperList.map((symbol) => ({ symbol, mode: "paper" as Mode })),
    ...liveList.map((symbol) => ({ symbol, mode: "live" as Mode }))
  ];

  if (allSymbols.length === 0) {
    return <p className="empty-state" style={{ width: "100%" }}>구독 중인 종목이 없습니다.</p>;
  }

  return (
    <div className="chips-container">
      {allSymbols.map(({ symbol, mode }) => (
        <Chip key={`${mode}-${symbol}`} statusColor={mode === "live" ? "var(--status-error)" : "var(--status-success)"}>
          <div style={{ display: "flex", flexDirection: "column", gap: "2px", flex: 1 }}>
            <span style={{ fontWeight: 800, fontSize: "14px", color: "var(--text-primary)" }}>{symbol}</span>
            {symbolNameMap && symbolNameMap[symbol] && (
              <span style={{ color: "var(--text-secondary)", fontSize: "11px", fontWeight: 500 }}>
                {symbolNameMap[symbol]}
              </span>
            )}
          </div>
          <span
            style={{
              marginLeft: "auto",
              fontSize: "10px",
              padding: "2px 6px",
              background: "rgba(255,255,255,0.08)",
              borderRadius: "4px",
              color: "var(--text-muted)",
              fontWeight: 700
            }}
          >
            {mode.toUpperCase()}
          </span>
        </Chip>
      ))}
    </div>
  );
}
