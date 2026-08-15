import { useState } from "react";
import { KisPanel } from "../../../features/kis-management/ui/KisPanel";
import { UpbitPanel } from "../../../features/upbit-management/ui/UpbitPanel";
import { PageHeader } from "../../../shared/ui/PageHeader";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/shadcn/tabs";

export function RealtimePage() {
  const [tab, setTab] = useState<"kis" | "upbit">("kis");

  return (
    <section className="flex flex-col gap-4 sm:gap-5 animate-fade-in">
      <PageHeader
        eyebrow="시장 데이터"
        title="실시간 데이터"
        description="KIS 국내 주식과 Upbit 가상화폐의 실시간 수집 설정을 운영합니다."
      />
      <Tabs value={tab} onValueChange={(value) => setTab(value as "kis" | "upbit")}>
        <TabsList className="h-auto max-w-full flex-wrap">
          <TabsTrigger value="kis" className="px-4 py-2 sm:px-6">
            국내 주식 (KIS)
          </TabsTrigger>
          <TabsTrigger value="upbit" className="px-4 py-2 sm:px-6">
            가상화폐 (Upbit)
          </TabsTrigger>
        </TabsList>
      </Tabs>
      {tab === "kis" ? <KisPanel /> : <UpbitPanel />}
    </section>
  );
}
