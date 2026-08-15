import { useState } from "react";
import { SymbolCatalogPanel } from "../../../features/catalog-management/ui/SymbolCatalogPanel";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/shadcn/tabs";

export function HistoricalPage() {
  const [tab, setTab] = useState<"pykrx" | "yfinance">("pykrx");

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <Tabs value={tab} onValueChange={(value) => setTab(value as "pykrx" | "yfinance")}>
          <TabsList className="mb-2 h-auto">
            <TabsTrigger value="pykrx" className="px-6 py-2">
            국내 시장 (pykrx)
            </TabsTrigger>
            <TabsTrigger value="yfinance" className="px-6 py-2">
            해외 시장 (yfinance)
            </TabsTrigger>
          </TabsList>
        </Tabs>
        <p className="max-w-3xl text-sm text-muted-foreground sm:text-base">
          백테스트를 위한 과거 시세(OHLCV) 관리. 종목 카탈로그 구독, 수집 상태 관리 및 시세 조회 API 기능을 제공합니다.
        </p>
      </div>
      <SymbolCatalogPanel isPykrx={tab === "pykrx"} title={tab === "pykrx" ? "국내 카탈로그" : "해외 카탈로그"} />
    </section>
  );
}
