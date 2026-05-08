import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, DollarSign, RefreshCw, Search } from "lucide-react";
import type { CatalogResponse, KrSymbol, Mode } from "../../../entities/symbol/model/types";
import { fetchJson, normalizeByModes, type ModeSubscriptions } from "../../../shared/api";
import { fetchSubscriptionStatus } from "../../../shared/api/subscriptionStatusApi";
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

const INPUT_CLS = "bg-bg-input border border-border-primary text-text-primary px-4 py-3 rounded-xl outline-none transition-all w-full focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20 focus:bg-white";
const SELECT_CLS = "bg-bg-input border border-border-primary text-text-primary px-4 py-3 rounded-xl outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20 focus:bg-white";
const BTN_BASE = "inline-flex items-center justify-center gap-2.5 px-5 py-3 rounded-xl font-semibold text-sm cursor-pointer transition-all border whitespace-nowrap";
const BTN_PRIMARY = `${BTN_BASE} bg-brand-primary text-white shadow-sm border-transparent hover:bg-brand-primary/90`;
const BTN_OUTLINE = `${BTN_BASE} bg-white border-border-primary text-text-primary hover:bg-bg-input hover:border-text-muted`;
const BTN_DANGER = `${BTN_BASE} bg-red-50 text-red-600 border-red-200 hover:bg-red-100 shadow-sm`;

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
    staleTime: 0
  });
  const {
    data: subscriptionStatus,
    isLoading: isSubscriptionStatusLoading,
    isError: isSubscriptionStatusError,
    error: subscriptionStatusError,
    refetch: refetchSubscriptionStatus
  } = useQuery({
    queryKey: ["kis", "subscription-status"],
    queryFn: fetchSubscriptionStatus,
    refetchInterval: 10_000
  });

  const symbolNameMap = useMemo(() => {
    const map: Record<string, string> = {};
    catalog.items.forEach(item => {
      map[item.symbol] = item.name;
    });
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

  const loading =
    isOldListLoading ||
    isCatalogLoading ||
    isSubscriptionsLoading ||
    subscriptionMutation.isPending ||
    isPriceLoading ||
    isSubscriptionStatusLoading;

  const getStatusMessage = () => {
    if (subscriptionMutation.isError) return "요청 실패";
    if (subscriptionMutation.isSuccess) return "구독 상태가 변경되었습니다.";
    if (isPriceLoading) return "현재가 조회 중...";
    return "시스템 준비 완료";
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Summary strip */}
      <div className="grid grid-cols-3 gap-3 max-lg:grid-cols-1">
        <div className="border border-border-primary rounded-[16px] bg-bg-card shadow-sm px-5 py-4 flex flex-col gap-1">
          <span className="text-text-muted text-[13px] font-medium">계좌 모드</span>
          <strong className="text-xl text-brand-primary tracking-tight">{mode === "paper" ? "모의투자" : "실전투자"}</strong>
        </div>
        <div className="border border-border-primary rounded-[16px] bg-bg-card shadow-sm px-5 py-4 flex flex-col gap-1">
          <span className="text-text-muted text-[13px] font-medium">수집 채널</span>
          <strong className="text-xl text-brand-primary tracking-tight">{channel === "ws" ? "실시간 (WS)" : "일반 (REST)"}</strong>
        </div>
        <div className="border border-border-primary rounded-[16px] bg-bg-card shadow-sm px-5 py-4 flex flex-col gap-1">
          <span className="text-text-muted text-[13px] font-medium">현재 구독 수</span>
          <strong className="text-xl text-brand-primary tracking-tight">{modeSubscriptions.items?.length ?? 0} 건</strong>
        </div>
      </div>

      <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
        <SectionCard
          title="한국투자증권(KIS) 종목 카탈로그"
          icon={Search}
          headerAction={(
            <div className="flex gap-3 flex-wrap">
              <select className={`${SELECT_CLS} w-[120px]`} value={mode} onChange={(e) => setMode(e.target.value as Mode)}>
                <option value="paper">모의투자</option>
                <option value="live">실전투자</option>
              </select>
              <select className={`${SELECT_CLS} w-[100px]`} value={channel} onChange={(e) => setChannel(e.target.value as KisChannel)}>
                <option value="ws">WS</option>
                <option value="rest">REST</option>
              </select>
              <input className={`${INPUT_CLS} w-[180px]`} value={query} onChange={(e) => setQuery(e.target.value)} placeholder="종목코드 또는 이름" />
              <select className={`${SELECT_CLS} w-[120px]`} value={market} onChange={(e) => setMarket(e.target.value)}>
                <option value="">전체 시장</option>
                <option value="KOSPI">KOSPI</option>
                <option value="KOSDAQ">KOSDAQ</option>
              </select>
              <select className={`${SELECT_CLS} w-[140px]`} value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}>
                <option value="">전체 상태</option>
                <option value="subscribed">구독 중</option>
                <option value="unsubscribed">미구독</option>
              </select>
            </div>
          )}
        >
          <div className="flex gap-2.5 flex-wrap px-6 pb-3.5">
            <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">전체 카탈로그: {catalog.totalCatalogCount}</span>
            <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">전체 구독 중: {catalog.totalSubscribedCount}</span>
            <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">조회 결과: {catalog.returnedCount}</span>
          </div>
          <KisSearchList
            results={catalog.items}
            onSelect={(selectedSymbol) => {
              setSymbol(selectedSymbol);
            }}
          />
          {catalog.items.length < catalog.totalCatalogCount && (
            <button
              className="load-more-btn"
              onClick={() => setCatalogLimit(prev => prev + 20)}
            >
              <RefreshCw size={14} /> 더보기 ({catalog.items.length} / {catalog.totalCatalogCount})
            </button>
          )}
        </SectionCard>

        <SectionCard title="KIS 구독 제어" icon={Activity}>
          <div className="px-6 py-4 flex flex-col gap-3">
            <div className="flex gap-3 flex-wrap">
              <input
                className={`${INPUT_CLS} w-[150px]`}
                value={symbol}
                onChange={(e) => setSymbol(e.target.value.toUpperCase())}
                placeholder="종목코드"
              />
              <button className={BTN_PRIMARY} onClick={() => symbol && subscriptionMutation.mutate({ action: "add", targetSymbol: symbol })}>
                구독 추가
              </button>
              <button className={BTN_DANGER} onClick={() => symbol && subscriptionMutation.mutate({ action: "remove", targetSymbol: symbol })}>
                구독 해지
              </button>
            </div>
            <div className="flex gap-2.5 flex-wrap">
              <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">선택된 종목: {symbol || "-"}</span>
              <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">구독 중인 종목: {selectedSet.size}건</span>
            </div>
            <div className="flex flex-col">
              <KisModeList data={asModeSubscriptions(modeSubscriptions.items ?? [], mode)} symbolNameMap={symbolNameMap} />
            </div>
          </div>
        </SectionCard>
      </div>

      <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
        <SectionCard title="KIS 실시간 시세 조회 (REST)" icon={DollarSign}>
          <div className="px-6 py-4 flex flex-col gap-3">
            <div className="flex gap-3 flex-wrap">
              <input
                className={`${INPUT_CLS} w-[150px]`}
                value={priceSymbol}
                onChange={(e) => setPriceSymbol(e.target.value.toUpperCase())}
                placeholder="종목코드"
              />
              <button className={BTN_OUTLINE} onClick={() => void refetchPrice()}>
                현재가 조회
              </button>
            </div>
            <div className="flex gap-2.5 flex-wrap">
              <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">조회 결과 로드: {priceResult ? "성공" : "없음"}</span>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="레거시 API 연결 상태 (WS/REST 목록)" icon={Activity}>
          <div className="px-6 py-4 flex flex-col gap-2.5">
            <div className="flex gap-2.5 flex-wrap">
              <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">모의투자 구독: {oldModeSubscriptions.paper.length}건</span>
              <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">실전투자 구독: {oldModeSubscriptions.live.length}건</span>
              <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">엔드포인트: {oldPath}</span>
            </div>
          </div>
        </SectionCard>
      </div>

      <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
        <SectionCard
          title="구독 상태 모니터링 (읽기 전용)"
          icon={Activity}
          headerAction={(
            <button className={BTN_OUTLINE} onClick={() => void refetchSubscriptionStatus()}>
              새로고침
            </button>
          )}
        >
          <div className="px-6 py-4 flex flex-col gap-3">
            {isSubscriptionStatusLoading && <div>로딩 중</div>}
            {isSubscriptionStatusError && (
              <div>상태 조회 실패: {subscriptionStatusError instanceof Error ? subscriptionStatusError.message : "알 수 없는 오류"}</div>
            )}
            {!isSubscriptionStatusLoading && !isSubscriptionStatusError && subscriptionStatus && (
              <>
                <div className="flex gap-2.5 flex-wrap">
                  <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">생성 시각: {subscriptionStatus.generatedAt}</span>
                  <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">전역 WS 슬롯: {subscriptionStatus.totalWsSlotUsed} / {subscriptionStatus.totalWsSlotMax}</span>
                </div>
                {subscriptionStatus.modes.length === 0 && <div>모드 상태 데이터가 없습니다.</div>}
                {subscriptionStatus.modes.map((item) => (
                  <div key={item.mode} className="border border-border-primary rounded-[8px] p-3 grid gap-2">
                    <div className="flex gap-2.5 flex-wrap">
                      <strong>{item.mode}</strong>
                      <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">{item.connectionStatus}</span>
                      <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">마지막 연결: {item.lastConnectedAt ?? "-"}</span>
                      <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">재연결 횟수: {item.reconnectAttempts}</span>
                      <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">WS 슬롯: {item.wsSlotUsed} / {item.wsSlotMax}</span>
                    </div>
                    <div className="flex gap-2.5 flex-wrap">
                      <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">WS 심볼({item.wsSymbols.length}): {item.wsSymbols.join(", ") || "-"}</span>
                    </div>
                    <div className="flex gap-2.5 flex-wrap">
                      <span className="text-xs text-text-secondary bg-bg-input rounded-full px-3 py-1.5 font-medium">REST 심볼({item.restSymbols.length}): {item.restSymbols.join(", ") || "-"}</span>
                    </div>
                  </div>
                ))}
              </>
            )}
          </div>
        </SectionCard>
      </div>

      <StatusBar message={getStatusMessage()} loading={loading} />
    </div>
  );
}
