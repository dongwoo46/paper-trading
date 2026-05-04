import type { TradingJournalListItem } from "../../../entities/trading-journal/model/types";

type Props = {
  items: TradingJournalListItem[];
  selectedJournalId: number | null;
  isLoading: boolean;
  isError: boolean;
  onSelect: (journalId: number) => void;
};

export function TradingJournalListPanel({ items, selectedJournalId, isLoading, isError, onSelect }: Props) {
  return (
    <section className="panel">
      <h3>거래 일지 목록</h3>
      {isLoading && <div style={{ padding: 16 }}>목록을 불러오는 중...</div>}
      {isError && <div style={{ padding: 16, color: "#ef4444" }}>목록을 불러오지 못했습니다.</div>}
      {!isLoading && !isError && items.length === 0 && <div style={{ padding: 16 }}>거래 일지가 없습니다.</div>}
      {!isLoading && !isError && items.length > 0 && (
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {items.map((item) => (
            <li key={item.journalId} style={{ marginBottom: 8 }}>
              <button
                type="button"
                onClick={() => onSelect(item.journalId)}
                style={{ width: "100%", textAlign: "left", fontWeight: selectedJournalId === item.journalId ? 700 : 400 }}
              >
                {item.ticker}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

