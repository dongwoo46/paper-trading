import { NavLink } from "react-router-dom";
import { Activity, ArrowRight, Database, Globe, History, Info, Zap } from "lucide-react";
import { Card, Chip, GlassPanel } from "../../../shared/ui";

export function HomePage() {
  return (
    <section className="panel">
      <div className="panel-header">
        <h2>트레이딩 데이터 콘솔</h2>
        <p className="lead">실시간 데이터 수집, 백테스트용 히스토리, 거시경제 지표를 한 화면에서 관리합니다.</p>
      </div>

      <div className="card-grid">
        <Card>
          <h3><Zap size={20} className="brand-icon" /> 실시간 데이터</h3>
          <p>KIS와 업비트의 실시간 구독 항목을 관리합니다. 채널별로 구독/해지 상태를 즉시 반영할 수 있습니다.</p>
          <NavLink to="/realtime" className="card-link">
            실시간 페이지 이동 <ArrowRight size={16} />
          </NavLink>
        </Card>

        <Card>
          <h3><History size={20} className="brand-icon" /> 과거 OHLCV</h3>
          <p>국내외 카탈로그에서 백테스트용 일봉 수집 대상을 선택하고 운영합니다. 시세 조회 API와 연동됩니다.</p>
          <NavLink to="/historical" className="card-link">
            히스토리 페이지 이동 <ArrowRight size={16} />
          </NavLink>
        </Card>

        <Card>
          <h3><Globe size={20} className="brand-icon" /> 거시경제 지표</h3>
          <p>FRED의 핵심 경제 지표를 탐색하고 구독할 수 있으며, 최신 관측 데이터를 즉시 조회합니다.</p>
          <NavLink to="/macro" className="card-link">
            매크로 페이지 이동 <ArrowRight size={16} />
          </NavLink>
        </Card>

        <Card>
          <h3><Database size={20} className="brand-icon" /> 통합 모니터링</h3>
          <p>전체 카탈로그 규모, 실시간 구독 현황, 필터링된 결과 개수를 실시간으로 모니터링합니다.</p>
          <Chip style={{ width: "fit-content", background: "rgba(16, 185, 129, 0.12)", color: "#10b981", border: "none" }}>
            <Activity size={14} /> 시스템 운영 준비 완료
          </Chip>
        </Card>
      </div>

      <GlassPanel style={{ padding: "32px", borderRadius: "24px", marginTop: "12px" }}>
        <h3 style={{ marginBottom: "12px", display: "flex", alignItems: "center", gap: "10px" }}>
          <Info size={20} color="#3b82f6" />
          운영 가이드
        </h3>
        <p style={{ color: "#94a3b8", fontSize: "15px", maxWidth: "860px", lineHeight: "1.7" }}>
          1. 사이드바 메뉴를 통해 관리하고자 하는 데이터 영역(실시간/히스토리/매크로)으로 이동합니다.<br />
          2. 각 데이터 소스의 카탈로그에서 검색과 필터를 적용하여 수집할 항목을 선택합니다.<br />
          3. 구독 버튼을 클릭하면 서버에 상태가 즉시 반영되며, 우측의 선택 목록에서 현재 구독 중인 항목을 확인할 수 있습니다.
        </p>
      </GlassPanel>
    </section>
  );
}
