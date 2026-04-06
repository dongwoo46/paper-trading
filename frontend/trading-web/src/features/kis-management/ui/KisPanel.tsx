import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, DollarSign, RefreshCw, Search } from "lucide-react";
import type { CatalogResponse, KrSymbol, Mode } from "../../../entities/symbol/model/types";
import { fetchJson, normalizeByModes, type ModeSubscriptions } from "../../../shared/api";
import { SectionCard, StatusBar } from "../../../shared/ui";
import { KisModeList } from "./KisModeList";
import { KisSearchList } from "./KisSearchList";

type KisChannel = "ws" | "rest";
type StatusFilter = "" | "subscribed" | "unsubscribed";

const EMPTY_KIS_CATALOG: CatalogResponse<KrSymbol> = {
  items: [],
  returnedCount: 0,
  totalCatalogCount: 0,
  totalSubscribedCount: 0
};

export function KisPanel() {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<Mode>("paper");
  const [channel, setChannel] = useState<KisChannel>("ws");
  const [symbol, setSymbol] = useState("");
  const [query, setQuery] = useState("");
  const [market, setMarket] = useState("");
  const [status, setStatus] = useState<StatusFilter>("");
  const [priceSymbol, setPriceSymbol] = useState("");
  const [catalogLimit, setCatalogLimit] = useState(20);

  const oldPath = channel === "ws" ? "/api/kis/ws/subscriptions" : "/api/kis/rest/watchlist";

  const { data: oldModeSubscriptions = { paper: [], live: [] }, isLoading: isOldListLoading } = useQuery({
    queryKey: ["kis", "old", channel, "all"],
    queryFn: () => fetchJson<Record<string, string[]>>(oldPath).then(normalizeByModes)
  });

  const { data: catalog = EMPTY_KIS_CATALOG, isLoading: isCatalogLoading } = useQuery({
    queryKey: ["kis", "catalog", mode, channel, query, market, status],
    queryFn: () => {
      const params = new URLSearchParams({
        mode,
        channel,
        limit: catalogLimit.toString()
      });
      if (query.trim()) params.set("query", query.trim());
      if (market.trim()) params.set("market", market.trim());
      if (status) params.set("status", status);
      return fetchJson<CatalogResponse<KrSymbol>>(`/api/kis/symbols/catalog?${params.toString()}`);
    }
  });

  const { data: modeSubscriptions = { items: [] as string[] }, isLoading: isSubscriptionsLoading } = useQuery({
    queryKey: ["kis", mode, channel, "subscriptions"],
    queryFn: () => fetchJson<{ items: string[]; returnedCount: number }>(`/api/kis/symbols/subscriptions?mode=${mode}&channel=${channel}`),
    staleTime: 0 // Ensure we get fresh data on channel switch
  });

  const symbolNameMap = useMemo(() => {
    const map: Record<string, string> = {};
    catalog.items.forEach(item => {
      map[item.symbol] = item.name;
    });
    // Fallback for those not in current catalog page but possibly in old search
    return map;
  }, [catalog.items]);

  const { data: priceResult, refetch: refetchPrice, isFetching: isPriceLoading } = useQuery({
    queryKey: ["kis", mode, priceSymbol, "price"],
    queryFn: () => fetchJson<Record<string, unknown>>(`/api/kis/rest/watchlist/price?mode=${mode}&symbol=${encodeURIComponent(priceSymbol)}`),
    enabled: false
  });

  const subscriptionMutation = useMutation({
    mutationFn: ({ action, targetSymbol }: { action: "add" | "remove"; targetSymbol: string }) =>
      fetchJson<{ status: string; totalSelected: number }>("/api/kis/symbols/subscriptions", {
        method: action === "add" ? "POST" : "DELETE",
        body: JSON.stringify({ mode, channel, symbol: targetSymbol.trim() })
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["kis"] });
    }
  });

  const selectedSet = useMemo(() => new Set(modeSubscriptions.items ?? []), [modeSubscriptions.items]);

  const asModeSubscriptions = (items: string[], selectedMode: Mode): ModeSubscriptions => {
    return selectedMode === "paper" ? { paper: items, live: [] } : { paper: [], live: items };
  };

  const loading = isOldListLoading || isCatalogLoading || isSubscriptionsLoading || subscriptionMutation.isPending || isPriceLoading;
  
  const getStatusMessage = () => {
    if (subscriptionMutation.isError) return "요청 실패";
    if (subscriptionMutation.isSuccess) return "구독 상태가 변경되었습니다.";
    if (isPriceLoading) return "현재가 조회 중...";
    return "시스템 준비 완료";
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="summary-strip">
        <div className="summary-item">
          <span>계좌 모드</span>
          <strong>{mode === "paper" ? "모의투자" : "실전투자"}</strong>
        </div>
        <div className="summary-item">
          <span>수집 채널</span>
          <strong>{channel === "ws" ? "실시간 (WS)" : "일반 (REST)"}</strong>
        </div>
        <div className="summary-item">
          <span>현재 구독 수</span>
          <strong>{modeSubscriptions.items?.length ?? 0} 건</strong>
        </div>
      </div>

      <div className="feature-grid">
        <SectionCard
          title="한국투자증권(KIS) 종목 카탈로그"
          icon={Search}
          headerAction={(
            <div className="form-row">
              <select value={mode} onChange={(e) => setMode(e.target.value as Mode)} style={{ width: "120px" }}>
                <option value="paper">모의투자</option>
                <option value="live">실전투자</option>
              </select>
              <select value={channel} onChange={(e) => setChannel(e.target.value as KisChannel)} style={{ width: "100px" }}>
                <option value="ws">WS</option>
                <option value="rest">REST</option>
              </select>
              <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="종목코드 또는 이름" style={{ width: "180px" }} />
              <select value={market} onChange={(e) => setMarket(e.target.value)} style={{ width: "120px" }}>
                <option value="">전체 시장</option>
                <option value="KOSPI">KOSPI</option>
                <option value="KOSDAQ">KOSDAQ</option>
              </select>
              <select value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)} style={{ width: "140px" }}>
                <option value="">전체 상태</option>
                <option value="subscribed">구독 중</option>
                <option value="unsubscribed">미구독</option>
              </select>
            </div>
          )}
        >
          <div className="meta-row">
            <span>전체 카탈로그: {catalog.totalCatalogCount}</span>
            <span>전체 구독 중: {catalog.totalSubscribedCount}</span>
            <span>조회 결과: {catalog.returnedCount}</span>
          </div>
          <KisSearchList
            results={catalog.items}
            onSelect={(selectedSymbol) => {
              setSymbol(selectedSymbol);
            }}
          />
          {catalog.items.length < catalog.totalCatalogCount && (
            <button className="load-more-btn" onClick={() => setCatalogLimit(prev => prev + 20)}>
              <RefreshCw size={14} /> 더보기 ({catalog.items.length} / {catalog.totalCatalogCount})
            </button>
          )}
        </SectionCard>

        <SectionCard title="KIS 구독 제어" icon={Activity}>
          <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <div className="form-row">
              <input value={symbol} onChange={(e) => setSymbol(e.target.value.toUpperCase())} placeholder="종목코드" style={{ width: "150px" }} />
              <button className="btn btn-primary" onClick={() => symbol && subscriptionMutation.mutate({ action: "add", targetSymbol: symbol })}>
                구독 추가
              </button>
              <button className="btn btn-danger" onClick={() => symbol && subscriptionMutation.mutate({ action: "remove", targetSymbol: symbol })}>
                구독 해지
              </button>
            </div>
            <div className="meta-row" style={{ padding: 0 }}>
              <span>선택된 종목: {symbol || "-"}</span>
              <span>구독 중인 종목: {selectedSet.size}건</span>
            </div>
            <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
              <KisModeList data={asModeSubscriptions(modeSubscriptions.items ?? [], mode)} symbolNameMap={symbolNameMap} />
            </div>
          </div>
        </SectionCard>
      </div>

      <div className="feature-grid">
        <SectionCard title="KIS 실시간 시세 조회 (REST)" icon={DollarSign}>
          <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <div className="form-row">
              <input value={priceSymbol} onChange={(e) => setPriceSymbol(e.target.value.toUpperCase())} placeholder="종목코드" style={{ width: "150px" }} />
              <button className="btn btn-outline" onClick={() => void refetchPrice()}>
                현재가 조회
              </button>
            </div>
            <div className="meta-row" style={{ padding: 0 }}>
              <span>조회 결과 로드: {priceResult ? "성공" : "없음"}</span>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="레거시 API 연결 상태 (WS/REST 목록)" icon={Activity}>
          <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "10px" }}>
            <div className="meta-row" style={{ padding: 0 }}>
              <span>모의투자 구독: {oldModeSubscriptions.paper.length}건</span>
              <span>실전투자 구독: {oldModeSubscriptions.live.length}건</span>
              <span>엔드포인트: {oldPath}</span>
            </div>
          </div>
        </SectionCard>
      </div>

      <StatusBar message={getStatusMessage()} loading={loading} />
    </div>
  );
}
