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
    <section className="panel" style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "end" }}>
        <label>
          티커
          <input aria-label="티커" value={ticker} onChange={(e) => onChangeTicker(e.target.value)} />
        </label>
        <label>
          시작일
          <input aria-label="시작일" type="date" value={from} onChange={(e) => onChangeFrom(e.target.value)} />
        </label>
        <label>
          종료일
          <input aria-label="종료일" type="date" value={to} onChange={(e) => onChangeTo(e.target.value)} />
        </label>
        <button type="button" onClick={onSubmit}>
          조회
        </button>
      </div>
    </section>
  );
}

