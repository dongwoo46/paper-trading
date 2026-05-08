import { useState } from "react";
import { KisPanel } from "../../../features/kis-management/ui/KisPanel";
import { UpbitPanel } from "../../../features/upbit-management/ui/UpbitPanel";

export function RealtimePage() {
  const [tab, setTab] = useState<"kis" | "upbit">("kis");

  return (
    <section className="flex flex-col gap-4 sm:gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <div className="inline-flex flex-wrap bg-white/[0.03] p-1 rounded-[16px] border border-white/12 mb-2 w-fit max-w-full">
          <button
            className={`px-4 sm:px-6 py-2.5 rounded-xl font-semibold text-xs sm:text-sm cursor-pointer transition-all whitespace-nowrap ${tab === "kis" ? "bg-bg-card text-brand-primary shadow-md border border-white/12" : "text-text-secondary hover:text-text-primary border border-transparent"}`}
            onClick={() => setTab("kis")}
          >
            국내 주식 (KIS)
          </button>
          <button
            className={`px-4 sm:px-6 py-2.5 rounded-xl font-semibold text-xs sm:text-sm cursor-pointer transition-all whitespace-nowrap ${tab === "upbit" ? "bg-bg-card text-brand-primary shadow-md border border-white/12" : "text-text-secondary hover:text-text-primary border border-transparent"}`}
            onClick={() => setTab("upbit")}
          >
            가상화폐 (Upbit)
          </button>
        </div>
        <p className="text-text-secondary text-sm sm:text-[15px] max-w-3xl">
          실시간 데이터 소스 관리. KIS 채널을 통한 국내 주식 구독 및 업비트 마켓의 가상화폐 실시간 수집 설정을 운영합니다.
        </p>
      </div>
      {tab === "kis" ? <KisPanel /> : <UpbitPanel />}
    </section>
  );
}
