import { Globe } from "lucide-react";
import { FredPanel } from "../../../features/fred-management/ui/FredPanel";

export function MacroPage() {
  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <div className="flex items-center gap-4">
          <Globe size={32} className="text-primary" />
          <h2 className="text-[28px] font-bold tracking-tight">거시경제 데이터 (FRED)</h2>
        </div>
        <p className="max-w-3xl text-sm text-muted-foreground sm:text-base">
          FRED 시리즈의 카탈로그 탐색, 구독 관리 및 데이터 동기화, 상세 정보와 관측치 조회를 위한 통합 API를 제공합니다.
        </p>
      </div>
      <FredPanel />
    </section>
  );
}
