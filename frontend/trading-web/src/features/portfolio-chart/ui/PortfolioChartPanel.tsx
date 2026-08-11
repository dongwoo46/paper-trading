import type { ReturnSeriesPoint } from "../../../entities/portfolio/model/types";
import { Alert, AlertDescription } from "@/shared/ui/shadcn/alert";
import { Card, CardContent } from "@/shared/ui/shadcn/card";
import { Skeleton } from "@/shared/ui/shadcn/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/ui/shadcn/table";

interface PortfolioChartPanelProps {
  series: ReturnSeriesPoint[];
  isLoading: boolean;
  isError: boolean;
  benchmarkWarning: boolean;
}

export function PortfolioChartPanel(props: PortfolioChartPanelProps) {
  if (props.isLoading) {
    return <Skeleton className="h-72 w-full" aria-label="차트 데이터를 불러오는 중" />;
  }

  if (props.isError) {
    return <Alert variant="destructive"><AlertDescription>차트 데이터를 불러오지 못했습니다.</AlertDescription></Alert>;
  }

  if (props.series.length === 0) {
    return <Alert><AlertDescription>표시할 차트 데이터가 없습니다.</AlertDescription></Alert>;
  }

  return (
    <Card>
      {props.benchmarkWarning && (
        <Alert className="mx-4 border-market-warning/30 bg-market-warning/10 text-market-warning">
          <AlertDescription>일부 날짜의 벤치마크 데이터가 없어 해당 포인트를 제외했습니다.</AlertDescription>
        </Alert>
      )}
      <CardContent className="px-0">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>날짜</TableHead>
              <TableHead className="text-right">평가금액</TableHead>
              <TableHead className="text-right">포트폴리오 수익률(%)</TableHead>
              <TableHead className="text-right">KOSPI 수익률(%)</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {props.series.map((row) => (
              <TableRow key={row.date}>
                <TableCell>{row.date}</TableCell>
                <TableCell className="text-right tabular-nums">{row.evaluationAmount.toLocaleString("ko-KR")}</TableCell>
                <TableCell className="text-right tabular-nums">{row.portfolioReturnPct.toFixed(2)}</TableCell>
                <TableCell className="text-right tabular-nums">{row.benchmarkReturnPct.toFixed(2)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
