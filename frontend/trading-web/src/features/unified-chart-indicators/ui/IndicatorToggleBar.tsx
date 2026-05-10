import type { UnifiedInterval, MarketSource } from "../../../shared/api/marketUnifiedApi";
import { useIndicatorStore } from "../model/useIndicatorStore";
import type { IndicatorKey } from "../model/useIndicatorStore";

interface IndicatorToggleBarProps {
  interval: UnifiedInterval;
  source: MarketSource;
}

type ButtonDef = {
  key: IndicatorKey;
  label: string;
  disabled: boolean;
};

const BTN_BASE =
  "inline-flex items-center justify-center gap-2.5 px-3 py-1.5 rounded-xl font-semibold text-xs cursor-pointer transition-all border whitespace-nowrap";
const BTN_ACTIVE = "border-blue-400 text-blue-100 bg-blue-500/[0.16]";
const BTN_INACTIVE =
  "bg-transparent border-white/12 text-text-primary hover:bg-white/5 hover:border-text-muted";
const BTN_DISABLED = "opacity-40 cursor-not-allowed";

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
          <button
            key={key}
            type="button"
            disabled={disabled}
            className={[
              BTN_BASE,
              isActive ? BTN_ACTIVE : BTN_INACTIVE,
              disabled ? BTN_DISABLED : "",
            ].join(" ")}
            onClick={() => {
              if (!disabled) toggle(key);
            }}
          >
            {label}
          </button>
        );
      })}
    </>
  );
}
