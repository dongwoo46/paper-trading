import { Globe } from "lucide-react";
import { FredPanel } from "../../../features/fred-management/ui/FredPanel";

export function MacroPage() {
  return (
    <section className="panel">
      <div className="panel-header">
        <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <Globe size={32} color="var(--brand-primary)" />
          <h2>거시경제 데이터 (FRED)</h2>
        </div>
        <p className="lead">
          FRED 시리즈의 카탈로그 탐색, 구독 관리 및 데이터 동기화, 상세 정보와 관측치 조회를 위한 통합 API를 제공합니다.
        </p>
      </div>
      <FredPanel />
    </section>
  );
}
