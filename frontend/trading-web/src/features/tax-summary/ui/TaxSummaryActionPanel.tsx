interface TaxSummaryActionPanelProps {
  isDisabled: boolean;
  onRecalculate: () => void;
}

export function TaxSummaryActionPanel({ isDisabled, onRecalculate }: TaxSummaryActionPanelProps) {
  return (
    <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: "16px" }}>
      <button onClick={onRecalculate} disabled={isDisabled}>
        세금 재계산
      </button>
    </div>
  );
}
