import { FredPanel } from "../../../features/fred-management/ui/FredPanel";
import { PageHeader } from "../../../shared/ui/PageHeader";

export function MacroPage() {
  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <PageHeader
        eyebrow="시장 데이터"
        title="거시경제 지표"
        description="FRED 시리즈를 탐색하고 구독·동기화 상태와 최신 관측치를 관리합니다."
      />
      <FredPanel />
    </section>
  );
}
