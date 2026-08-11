type Props = {
  ticker: string;
  from: string;
  to: string;
  onChangeTicker: (value: string) => void;
  onChangeFrom: (value: string) => void;
  onChangeTo: (value: string) => void;
  onSubmit: () => void;
};

export function TradingJournalFilterPanel({ ticker, from, to, onChangeTicker, onChangeFrom, onChangeTo, onSubmit }: Props) {
  return (
    <Card className="mb-4">
      <CardContent className="flex flex-wrap items-end gap-3">
        <div className="grid min-w-40 gap-1.5">
          <Label htmlFor="journal-ticker">티커</Label>
          <Input id="journal-ticker" aria-label="티커" value={ticker} onChange={(e) => onChangeTicker(e.target.value)} />
        </div>
        <div className="grid min-w-40 gap-1.5">
          <Label htmlFor="journal-from">시작일</Label>
          <Input id="journal-from" aria-label="시작일" type="date" value={from} onChange={(e) => onChangeFrom(e.target.value)} />
        </div>
        <div className="grid min-w-40 gap-1.5">
          <Label htmlFor="journal-to">종료일</Label>
          <Input id="journal-to" aria-label="종료일" type="date" value={to} onChange={(e) => onChangeTo(e.target.value)} />
        </div>
        <Button type="button" onClick={onSubmit}>
          조회
        </Button>
      </CardContent>
    </Card>
  );
}
import { Button } from "@/shared/ui/shadcn/button";
import { Card, CardContent } from "@/shared/ui/shadcn/card";
import { Input } from "@/shared/ui/shadcn/input";
import { Label } from "@/shared/ui/shadcn/label";

