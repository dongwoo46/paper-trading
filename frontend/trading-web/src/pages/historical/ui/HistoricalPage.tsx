import { useState } from "react";
import { SymbolCatalogPanel } from "../../../features/catalog-management/ui/SymbolCatalogPanel";

export function HistoricalPage() {
  const [tab, setTab] = useState<"pykrx" | "yfinance">("pykrx");

  return (
    <section className="panel">
      <div className="panel-header">
        <div className="tabs-container">
          <button className={`tab-btn ${tab === "pykrx" ? "active" : ""}`} onClick={() => setTab("pykrx")}>
            국내 시장 (pykrx)
          </button>
          <button className={`tab-btn ${tab === "yfinance" ? "active" : ""}`} onClick={() => setTab("yfinance")}>
            해외 시장 (yfinance)
          </button>
        </div>
        <p className="lead">
          백테스트를 위한 과거 시세(OHLCV) 관리. 종목 카탈로그 구독, 수집 상태 관리 및 시세 조회 API 기능을 제공합니다.
        </p>
      </div>
      <SymbolCatalogPanel isPykrx={tab === "pykrx"} title={tab === "pykrx" ? "국내 카탈로그" : "해외 카탈로그"} />
    </section>
  );
}
