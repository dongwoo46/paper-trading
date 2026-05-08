import { ChevronRight } from "lucide-react";
import type { KrSymbol } from "../../../entities/symbol/model/types";

interface Props {
  results: KrSymbol[];
  onSelect: (symbol: string) => void;
}

export function KisSearchList({ results, onSelect }: Props) {
  if (results.length === 0) {
    return <div className="py-10 px-6 text-center text-text-muted italic">검색 결과가 없습니다.</div>;
  }

  return (
    <div className="flex-1 overflow-y-auto min-h-[480px] flex flex-col">
      {results.map((row) => (
        <button
          key={row.symbol}
          className="w-full flex justify-between items-center px-5 py-3.5 text-left cursor-pointer border-b border-white/12 transition-all hover:bg-blue-500/5 hover:translate-x-1 gap-3 bg-transparent"
          onClick={() => onSelect(row.symbol)}
        >
          <span className="font-bold text-brand-primary min-w-[80px]">{row.symbol}</span>
          <span className="flex-1 whitespace-nowrap overflow-hidden text-ellipsis text-text-secondary">{row.name}</span>
          <span className="text-text-muted text-xs flex items-center gap-1">
            {row.market} <ChevronRight size={14} />
          </span>
        </button>
      ))}
    </div>
  );
}
