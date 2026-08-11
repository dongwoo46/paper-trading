import { Chip } from "../../../shared/ui";
import type { ModeSubscriptions } from "../../../shared/api";
import type { Mode } from "../../../entities/symbol/model/types";
import { Badge } from "@/shared/ui/shadcn/badge";

interface Props {
  data: ModeSubscriptions;
  symbolNameMap: Record<string, string>;
}

export function KisModeList({ data, symbolNameMap }: Props) {
  const paperList = Array.isArray(data?.paper) ? data.paper : [];
  const liveList = Array.isArray(data?.live) ? data.live : [];

  const allSymbols = [
    ...paperList.map((symbol) => ({ symbol, mode: "paper" as Mode })),
    ...liveList.map((symbol) => ({ symbol, mode: "live" as Mode }))
  ];

  if (allSymbols.length === 0) {
    return <p className="w-full px-6 py-10 text-center text-muted-foreground">구독 중인 종목이 없습니다.</p>;
  }

  return (
    <div className="flex flex-wrap gap-2.5 p-6">
      {allSymbols.map(({ symbol, mode }) => (
        <Chip key={`${mode}-${symbol}`} className="w-full justify-between">
          <span className={`size-2 rounded-full ${mode === "live" ? "bg-market-negative" : "bg-market-positive"}`} />
          <div className="flex flex-1 flex-col gap-0.5">
            <span className="text-sm font-extrabold text-foreground">{symbol}</span>
            {symbolNameMap && symbolNameMap[symbol] && (
              <span className="text-xs font-medium text-muted-foreground">
                {symbolNameMap[symbol]}
              </span>
            )}
          </div>
          <Badge variant="outline" className="ml-auto text-[10px]">
            {mode.toUpperCase()}
          </Badge>
        </Chip>
      ))}
    </div>
  );
}
