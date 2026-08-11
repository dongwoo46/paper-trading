import { useState } from "react";
import { KisPanel } from "../../../features/kis-management/ui/KisPanel";
import { UpbitPanel } from "../../../features/upbit-management/ui/UpbitPanel";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/shadcn/tabs";

export function RealtimePage() {
  const [tab, setTab] = useState<"kis" | "upbit">("kis");

  return (
    <section className="flex flex-col gap-4 sm:gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <Tabs value={tab} onValueChange={(value) => setTab(value as "kis" | "upbit")}>
          <TabsList className="mb-2 h-auto max-w-full flex-wrap">
            <TabsTrigger value="kis" className="px-4 py-2 sm:px-6">
            국내 주식 (KIS)
            </TabsTrigger>
            <TabsTrigger value="upbit" className="px-4 py-2 sm:px-6">
            가상화폐 (Upbit)
            </TabsTrigger>
          </TabsList>
        </Tabs>
        <p className="max-w-3xl text-sm text-muted-foreground sm:text-base">
          실시간 데이터 소스 관리. KIS 채널을 통한 국내 주식 구독 및 업비트 마켓의 가상화폐 실시간 수집 설정을 운영합니다.
        </p>
      </div>
      {tab === "kis" ? <KisPanel /> : <UpbitPanel />}
    </section>
  );
}
