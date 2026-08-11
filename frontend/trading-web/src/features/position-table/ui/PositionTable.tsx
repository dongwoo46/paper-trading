import type { PositionResponse } from "../../../entities/account/model/types";
import { cn } from "../../../shared/lib/utils";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../../shared/ui/shadcn/table";
import { formatAmount, formatRate } from "../../../shared/utils/format";

interface PositionTableProps {
  positions: PositionResponse[];
}

export function PositionTable({ positions }: PositionTableProps) {
  if (positions.length === 0) {
    return (
      <Table>
        <TableHeader>
          <PositionTableHead />
        </TableHeader>
        <TableBody>
          <TableRow>
            <TableCell
              colSpan={9}
              className="h-24 text-center text-muted-foreground"
            >
              포지션 없음
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    );
  }

  return (
    <Table>
      <TableHeader>
        <PositionTableHead />
      </TableHeader>
      <TableBody>
        {positions.map((pos) => (
          <PositionRow key={pos.ticker} position={pos} />
        ))}
      </TableBody>
    </Table>
  );
}

function PositionTableHead() {
  return (
    <TableRow>
      <TableHead>종목</TableHead>
      <TableHead>시장</TableHead>
      <TableHead className="text-right">수량</TableHead>
      <TableHead className="text-right">평균단가</TableHead>
      <TableHead className="text-right">현재가</TableHead>
      <TableHead className="text-right">평가금액</TableHead>
      <TableHead className="text-right">평가손익</TableHead>
      <TableHead className="text-right">수익률</TableHead>
      <TableHead>가격소스</TableHead>
    </TableRow>
  );
}

interface PositionRowProps {
  position: PositionResponse;
}

function PositionRow({ position }: PositionRowProps) {
  const returnRateFloat = position.returnRate !== null ? parseFloat(position.returnRate) : null;
  const unrealizedPnlFloat = position.unrealizedPnl !== null ? parseFloat(position.unrealizedPnl) : null;

  const tone = returnRateFloat === null ? null : returnRateFloat >= 0 ? "positive" : "negative";
  const toneClass = tone === "positive"
    ? "text-market-positive"
    : tone === "negative"
      ? "text-market-negative"
      : "text-muted-foreground";

  const unrealizedPnlDisplay =
    position.unrealizedPnl === null
      ? "-"
      : unrealizedPnlFloat !== null && unrealizedPnlFloat > 0
        ? "+" + formatAmount(position.unrealizedPnl)
        : formatAmount(position.unrealizedPnl);

  return (
    <TableRow>
      <TableCell className="font-medium">{position.ticker}</TableCell>
      <TableCell>{position.marketType}</TableCell>
      <TableCell className="text-right">{position.quantity}</TableCell>
      <TableCell className="text-right">{formatAmount(position.avgBuyPrice)}</TableCell>
      <TableCell className="text-right">{formatAmount(position.currentPrice)}</TableCell>
      <TableCell className="text-right">{formatAmount(position.evaluationAmount)}</TableCell>
      <TableCell className={cn("text-right", toneClass)} data-tone={tone ?? undefined}>
        {unrealizedPnlDisplay}
      </TableCell>
      <TableCell className={cn("text-right", toneClass)} data-tone={tone ?? undefined}>
        {formatRate(position.returnRate)}
      </TableCell>
      <TableCell>{position.priceSource}</TableCell>
    </TableRow>
  );
}
