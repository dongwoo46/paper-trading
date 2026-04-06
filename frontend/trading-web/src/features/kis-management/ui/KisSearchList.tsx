import { ChevronRight } from "lucide-react";
import type { KrSymbol } from "../../../entities/symbol/model/types";

interface Props {
  results: KrSymbol[];
  onSelect: (symbol: string) => void;
}

export function KisSearchList({ results, onSelect }: Props) {
  if (results.length === 0) {
    return <div className="empty-state">검색 결과가 없습니다.</div>;
  }

  return (
    <div className="result-list">
      {results.map((row) => (
        <button key={row.symbol} className="result-item" onClick={() => onSelect(row.symbol)}>
          <span className="symbol">{row.symbol}</span>
          <span className="name" style={{ color: "var(--text-secondary)" }}>{row.name}</span>
          <span className="market">
            {row.market} <ChevronRight size={14} />
          </span>
        </button>
      ))}
    </div>
  );
}
