import { NavLink } from "react-router-dom";
import { Activity, ArrowRight, Database, Globe, History, Info, Zap } from "lucide-react";
import { Card, Chip, GlassPanel } from "../../../shared/ui";
import { Button } from "@/shared/ui/shadcn/button";

export function HomePage() {
  return (
    <section className="flex flex-col gap-4 sm:gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <h2 className="text-2xl sm:text-[28px] font-bold tracking-tight">트레이딩 데이터 콘솔</h2>
        <p className="max-w-3xl text-sm text-muted-foreground sm:text-base">실시간 데이터 수집, 백테스트용 히스토리, 거시경제 지표를 한 화면에서 관리합니다.</p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4 sm:gap-5">
        <Card>
          <h3 className="text-lg sm:text-[20px] font-semibold flex items-center gap-2.5">
            <Zap size={20} className="text-primary" /> 실시간 데이터
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-muted-foreground sm:text-base">
            KIS와 업비트의 실시간 구독 항목을 관리합니다. 채널별로 구독/해지 상태를 즉시 반영할 수 있습니다.
          </p>
          <Button render={<NavLink to="/realtime" />} variant="link" className="mt-4 w-fit px-0">
            실시간 페이지 이동 <ArrowRight size={16} />
          </Button>
        </Card>

        <Card>
          <h3 className="text-lg sm:text-[20px] font-semibold flex items-center gap-2.5">
            <History size={20} className="text-primary" /> 과거 OHLCV
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-muted-foreground sm:text-base">
            국내외 카탈로그에서 백테스트용 일봉 수집 대상을 선택하고 운영합니다. 시세 조회 API와 연동됩니다.
          </p>
          <Button render={<NavLink to="/historical" />} variant="link" className="mt-4 w-fit px-0">
            히스토리 페이지 이동 <ArrowRight size={16} />
          </Button>
        </Card>

        <Card>
          <h3 className="text-lg sm:text-[20px] font-semibold flex items-center gap-2.5">
            <Globe size={20} className="text-primary" /> 거시경제 지표
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-muted-foreground sm:text-base">
            FRED의 핵심 경제 지표를 탐색하고 구독할 수 있으며, 최신 관측 데이터를 즉시 조회합니다.
          </p>
          <Button render={<NavLink to="/macro" />} variant="link" className="mt-4 w-fit px-0">
            매크로 페이지 이동 <ArrowRight size={16} />
          </Button>
        </Card>

        <Card>
          <h3 className="text-lg sm:text-[20px] font-semibold flex items-center gap-2.5">
            <Database size={20} className="text-primary" /> 통합 모니터링
          </h3>
          <p className="mt-2 text-sm leading-relaxed text-muted-foreground sm:text-base">
            전체 카탈로그 규모, 실시간 구독 현황, 필터링된 결과 개수를 실시간으로 모니터링합니다.
          </p>
          <Chip className="w-fit border-0 bg-market-positive/10 text-market-positive">
            <Activity size={14} /> 시스템 운영 준비 완료
          </Chip>
        </Card>
      </div>

      <GlassPanel className="mt-2 p-5 sm:mt-3 sm:p-8">
        <h3 className="mb-3 flex items-center gap-2.5">
          <Info size={20} className="text-primary" />
          운영 가이드
        </h3>
        <p className="max-w-4xl text-sm leading-relaxed text-muted-foreground sm:text-base">
          1. 사이드바 메뉴를 통해 관리하고자 하는 데이터 영역(실시간/히스토리/매크로)으로 이동합니다.<br />
          2. 각 데이터 소스의 카탈로그에서 검색과 필터를 적용하여 수집할 항목을 선택합니다.<br />
          3. 구독 버튼을 클릭하면 서버에 상태가 즉시 반영되며, 우측의 선택 목록에서 현재 구독 중인 항목을 확인할 수 있습니다.
        </p>
      </GlassPanel>
    </section>
  );
}
