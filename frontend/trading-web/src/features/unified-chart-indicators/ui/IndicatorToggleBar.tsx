import type { UnifiedInterval, MarketSource } from "../../../shared/api/marketUnifiedApi";
import { useIndicatorStore } from "../model/useIndicatorStore";
import type { IndicatorKey } from "../model/useIndicatorStore";
import { Button } from "@/shared/ui/shadcn/button";

interface IndicatorToggleBarProps {
  interval: UnifiedInterval;
  source: MarketSource;
}

type ButtonDef = {
  key: IndicatorKey;
  label: string;
  disabled: boolean;
};

export function IndicatorToggleBar({ interval, source }: IndicatorToggleBarProps) {
  const active = useIndicatorStore((s) => s.active);
  const toggle = useIndicatorStore((s) => s.toggle);

  const is1d = interval === "1d";
  const isPykrx1d = source === "pykrx" && interval === "1d";

  const buttons: ButtonDef[] = [
    { key: "volume", label: "거래량", disabled: false },
    { key: "ma5", label: "MA5", disabled: false },
    { key: "ma20", label: "MA20", disabled: false },
    { key: "ma60", label: "MA60", disabled: false },
    { key: "ma120", label: "MA120", disabled: false },
    { key: "bb", label: "BB", disabled: !is1d },
    { key: "rsi", label: "RSI", disabled: !is1d },
    { key: "macd", label: "MACD", disabled: !is1d },
    { key: "investorFlow", label: "수급", disabled: !isPykrx1d },
  ];

  return (
    <>
      {buttons.map(({ key, label, disabled }) => {
        const isActive = active[key];
        return (
          <Button
            key={key}
            type="button"
            disabled={disabled}
            size="sm"
            variant={isActive ? "default" : "outline"}
            onClick={() => {
              if (!disabled) toggle(key);
            }}
          >
            {label}
          </Button>
        );
      })}
    </>
  );
}
