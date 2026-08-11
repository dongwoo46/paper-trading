import { ChevronRight } from "lucide-react";
import type { KrSymbol } from "../../../entities/symbol/model/types";
import { Button } from "@/shared/ui/shadcn/button";

interface Props {
  results: KrSymbol[];
  onSelect: (symbol: string) => void;
}

export function KisSearchList({ results, onSelect }: Props) {
  if (results.length === 0) {
    return <div className="px-6 py-10 text-center text-muted-foreground">검색 결과가 없습니다.</div>;
  }

  return (
    <div className="flex-1 overflow-y-auto min-h-[480px] flex flex-col">
      {results.map((row) => (
        <Button
          key={row.symbol}
          type="button"
          variant="ghost"
          className="h-auto w-full justify-between rounded-none border-b px-5 py-3.5 text-left"
          onClick={() => onSelect(row.symbol)}
        >
          <span className="min-w-20 font-bold text-primary">{row.symbol}</span>
          <span className="flex-1 overflow-hidden text-ellipsis whitespace-nowrap text-muted-foreground">{row.name}</span>
          <span className="flex items-center gap-1 text-xs text-muted-foreground">
            {row.market} <ChevronRight size={14} />
          </span>
        </Button>
      ))}
    </div>
  );
}
