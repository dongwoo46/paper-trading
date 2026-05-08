import { useState } from "react";
import { SymbolCatalogPanel } from "../../../features/catalog-management/ui/SymbolCatalogPanel";

export function HistoricalPage() {
  const [tab, setTab] = useState<"pykrx" | "yfinance">("pykrx");

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <div className="inline-flex bg-white/[0.03] p-1 rounded-[16px] border border-white/12 mb-2">
          <button
            className={`px-6 py-2.5 rounded-xl font-semibold text-sm cursor-pointer transition-all ${tab === "pykrx" ? "bg-bg-card text-brand-primary shadow-md border border-white/12" : "text-text-secondary hover:text-text-primary border border-transparent"}`}
            onClick={() => setTab("pykrx")}
          >
            국내 시장 (pykrx)
          </button>
          <button
            className={`px-6 py-2.5 rounded-xl font-semibold text-sm cursor-pointer transition-all ${tab === "yfinance" ? "bg-bg-card text-brand-primary shadow-md border border-white/12" : "text-text-secondary hover:text-text-primary border border-transparent"}`}
            onClick={() => setTab("yfinance")}
          >
            해외 시장 (yfinance)
          </button>
        </div>
        <p className="text-text-secondary text-[15px] max-w-3xl">
          백테스트를 위한 과거 시세(OHLCV) 관리. 종목 카탈로그 구독, 수집 상태 관리 및 시세 조회 API 기능을 제공합니다.
        </p>
      </div>
      <SymbolCatalogPanel isPykrx={tab === "pykrx"} title={tab === "pykrx" ? "국내 카탈로그" : "해외 카탈로그"} />
    </section>
  );
}
