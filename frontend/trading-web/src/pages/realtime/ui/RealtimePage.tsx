import { useState } from "react";
import { KisPanel } from "../../../features/kis-management/ui/KisPanel";
import { UpbitPanel } from "../../../features/upbit-management/ui/UpbitPanel";

export function RealtimePage() {
  const [tab, setTab] = useState<"kis" | "upbit">("kis");

  return (
    <section className="panel">
      <div className="panel-header">
        <div className="tabs-container">
          <button className={`tab-btn ${tab === "kis" ? "active" : ""}`} onClick={() => setTab("kis")}>
            국내 주식 (KIS)
          </button>
          <button className={`tab-btn ${tab === "upbit" ? "active" : ""}`} onClick={() => setTab("upbit")}>
            가상화폐 (Upbit)
          </button>
        </div>
        <p className="lead">
          실시간 데이터 소스 관리. KIS 채널을 통한 국내 주식 구독 및 업비트 마켓의 가상화폐 실시간 수집 설정을 운영합니다.
        </p>
      </div>
      {tab === "kis" ? <KisPanel /> : <UpbitPanel />}
    </section>
  );
}
