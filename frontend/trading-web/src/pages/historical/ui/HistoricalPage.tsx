import { useState } from "react";
import { SymbolCatalogPanel } from "../../../features/catalog-management/ui/SymbolCatalogPanel";
import { PageHeader } from "../../../shared/ui/PageHeader";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/shadcn/tabs";

export function HistoricalPage() {
  const [tab, setTab] = useState<"pykrx" | "yfinance">("pykrx");

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <PageHeader
        eyebrow="시장 데이터"
        title="과거 시세 수집"
        description="백테스트용 과거 시세 카탈로그와 수집 상태를 관리합니다."
      />
      <Tabs value={tab} onValueChange={(value) => setTab(value as "pykrx" | "yfinance")}>
        <TabsList className="h-auto">
          <TabsTrigger value="pykrx" className="px-6 py-2">
            국내 시장 (pykrx)
          </TabsTrigger>
          <TabsTrigger value="yfinance" className="px-6 py-2">
            해외 시장 (yfinance)
          </TabsTrigger>
        </TabsList>
      </Tabs>
      <SymbolCatalogPanel isPykrx={tab === "pykrx"} title={tab === "pykrx" ? "국내 카탈로그" : "해외 카탈로그"} />
    </section>
  );
}
