import type { PositionResponse } from "../../../entities/account/model/types";
import { formatAmount, formatRate } from "../../../shared/utils/format";

interface PositionTableProps {
  positions: PositionResponse[];
}

export function PositionTable({ positions }: PositionTableProps) {
  if (positions.length === 0) {
    return (
      <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
        <thead>
          <PositionTableHead />
        </thead>
        <tbody>
          <tr>
            <td
              colSpan={9}
              style={{ textAlign: "center", padding: "32px", color: "var(--text-secondary, #6b7280)" }}
            >
              포지션 없음
            </td>
          </tr>
        </tbody>
      </table>
    );
  }

  return (
    <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
      <thead>
        <PositionTableHead />
      </thead>
      <tbody>
        {positions.map((pos) => (
          <PositionRow key={pos.ticker} position={pos} />
        ))}
      </tbody>
    </table>
  );
}

function PositionTableHead() {
  const thStyle: React.CSSProperties = {
    padding: "10px 12px",
    textAlign: "right",
    fontWeight: 600,
    color: "var(--text-secondary, #6b7280)",
    borderBottom: "1px solid var(--border, #e5e7eb)",
    whiteSpace: "nowrap",
  };
  const thLeftStyle: React.CSSProperties = { ...thStyle, textAlign: "left" };

  return (
    <tr>
      <th style={thLeftStyle}>종목</th>
      <th style={thLeftStyle}>시장</th>
      <th style={thStyle}>수량</th>
      <th style={thStyle}>평균단가</th>
      <th style={thStyle}>현재가</th>
      <th style={thStyle}>평가금액</th>
      <th style={thStyle}>평가손익</th>
      <th style={thStyle}>수익률</th>
      <th style={thLeftStyle}>가격소스</th>
    </tr>
  );
}

interface PositionRowProps {
  position: PositionResponse;
}

function PositionRow({ position }: PositionRowProps) {
  const tdStyle: React.CSSProperties = {
    padding: "10px 12px",
    textAlign: "right",
    borderBottom: "1px solid var(--border, #f3f4f6)",
  };
  const tdLeftStyle: React.CSSProperties = { ...tdStyle, textAlign: "left" };

  const returnRateFloat = position.returnRate !== null ? parseFloat(position.returnRate) : null;
  const unrealizedPnlFloat = position.unrealizedPnl !== null ? parseFloat(position.unrealizedPnl) : null;

  const returnRateColor =
    returnRateFloat === null ? undefined : returnRateFloat >= 0 ? "#10b981" : "#ef4444";

  const unrealizedPnlDisplay =
    position.unrealizedPnl === null
      ? "-"
      : unrealizedPnlFloat !== null && unrealizedPnlFloat > 0
        ? `+${formatAmount(position.unrealizedPnl)}`
        : formatAmount(position.unrealizedPnl);

  return (
    <tr>
      <td style={tdLeftStyle}>{position.ticker}</td>
      <td style={tdLeftStyle}>{position.marketType}</td>
      <td style={tdStyle}>{position.quantity}</td>
      <td style={tdStyle}>{formatAmount(position.avgBuyPrice)}</td>
      <td style={tdStyle}>{formatAmount(position.currentPrice)}</td>
      <td style={tdStyle}>{formatAmount(position.evaluationAmount)}</td>
      <td style={{ ...tdStyle, color: returnRateColor }}>{unrealizedPnlDisplay}</td>
      <td style={{ ...tdStyle, color: returnRateColor }}>{formatRate(position.returnRate)}</td>
      <td style={tdLeftStyle}>{position.priceSource}</td>
    </tr>
  );
}
