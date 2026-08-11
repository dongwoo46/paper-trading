import type { TradingJournalListItem } from "../../../entities/trading-journal/model/types";
import { Alert, AlertDescription } from "@/shared/ui/shadcn/alert";
import { Button } from "@/shared/ui/shadcn/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/shadcn/card";
import { Skeleton } from "@/shared/ui/shadcn/skeleton";

type Props = {
  items: TradingJournalListItem[];
  selectedJournalId: number | null;
  isLoading: boolean;
  isError: boolean;
  onSelect: (journalId: number) => void;
};

export function TradingJournalListPanel({ items, selectedJournalId, isLoading, isError, onSelect }: Props) {
  return (
    <Card>
      <CardHeader><CardTitle>거래 일지 목록</CardTitle></CardHeader>
      <CardContent>
      {isLoading && <Skeleton className="h-24 w-full" aria-label="거래 일지 목록 로딩 중" />}
      {isError && <Alert variant="destructive"><AlertDescription>목록을 불러오지 못했습니다.</AlertDescription></Alert>}
      {!isLoading && !isError && items.length === 0 && <Alert><AlertDescription>거래 일지가 없습니다.</AlertDescription></Alert>}
      {!isLoading && !isError && items.length > 0 && (
        <ul className="m-0 list-none p-0">
          {items.map((item) => (
            <li key={item.journalId} className="mb-2 last:mb-0">
              <Button
                type="button"
                variant={selectedJournalId === item.journalId ? "secondary" : "outline"}
                onClick={() => onSelect(item.journalId)}
                className="h-auto w-full justify-start py-2 text-left"
              >
                {item.ticker}
              </Button>
            </li>
          ))}
        </ul>
      )}
      </CardContent>
    </Card>
  );
}
