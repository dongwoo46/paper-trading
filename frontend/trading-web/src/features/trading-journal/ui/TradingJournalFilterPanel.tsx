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
    <section className="mb-4 rounded-[20px] border border-white/12 bg-bg-card p-4 sm:p-5">
      <div className="flex flex-wrap items-end gap-3">
        <label className="flex min-w-[160px] flex-col gap-1.5 text-sm text-text-secondary">
          티커
          <input className="h-10 rounded-lg border border-white/12 bg-bg-input px-3 text-text-primary outline-none focus:border-brand-primary" aria-label="티커" value={ticker} onChange={(e) => onChangeTicker(e.target.value)} />
        </label>
        <label className="flex min-w-[160px] flex-col gap-1.5 text-sm text-text-secondary">
          시작일
          <input className="h-10 rounded-lg border border-white/12 bg-bg-input px-3 text-text-primary outline-none focus:border-brand-primary" aria-label="시작일" type="date" value={from} onChange={(e) => onChangeFrom(e.target.value)} />
        </label>
        <label className="flex min-w-[160px] flex-col gap-1.5 text-sm text-text-secondary">
          종료일
          <input className="h-10 rounded-lg border border-white/12 bg-bg-input px-3 text-text-primary outline-none focus:border-brand-primary" aria-label="종료일" type="date" value={to} onChange={(e) => onChangeTo(e.target.value)} />
        </label>
        <button className="h-10 rounded-lg border border-brand-primary/40 bg-brand-primary/15 px-4 text-sm font-semibold text-brand-primary transition hover:bg-brand-primary/25" type="button" onClick={onSubmit}>
          조회
        </button>
      </div>
    </section>
  );
}

