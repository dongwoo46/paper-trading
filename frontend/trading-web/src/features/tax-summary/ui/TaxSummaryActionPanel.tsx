interface TaxSummaryActionPanelProps {
  isDisabled: boolean;
  onRecalculate: () => void;
}

export function TaxSummaryActionPanel({ isDisabled, onRecalculate }: TaxSummaryActionPanelProps) {
  return (
    <div className="mb-4 flex justify-end">
      <Button onClick={onRecalculate} disabled={isDisabled}>
        세금 재계산
      </Button>
    </div>
  );
}
import { Button } from "@/shared/ui/shadcn/button";
