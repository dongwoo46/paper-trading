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
    <section className="rounded-[20px] border border-white/12 bg-bg-card p-4 sm:p-5">
      <h3 className="mb-3 text-base font-semibold">거래 일지 목록</h3>
      {isLoading && <div className="p-4 text-sm text-text-secondary">목록을 불러오는 중...</div>}
      {isError && <div className="p-4 text-sm text-status-error">목록을 불러오지 못했습니다.</div>}
      {!isLoading && !isError && items.length === 0 && <div className="p-4 text-sm text-text-secondary">거래 일지가 없습니다.</div>}
      {!isLoading && !isError && items.length > 0 && (
        <ul className="m-0 list-none p-0">
          {items.map((item) => (
            <li key={item.journalId} className="mb-2 last:mb-0">
              <button
                type="button"
                onClick={() => onSelect(item.journalId)}
                className={`w-full rounded-lg border px-3 py-2 text-left text-sm transition ${
                  selectedJournalId === item.journalId
                    ? "border-brand-primary/40 bg-brand-primary/10 font-bold text-text-primary"
                    : "border-white/12 bg-bg-input font-normal text-text-secondary hover:text-text-primary"
                }`}
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

