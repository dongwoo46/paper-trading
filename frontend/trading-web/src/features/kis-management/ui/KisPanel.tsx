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
  const [manualActionMessage, setManualActionMessage] = useState<string | null>(null);
  const [lastAction, setLastAction] = useState<{ action: "add" | "remove"; symbol: string } | null>(null);
  const [favoriteSymbol, setFavoriteSymbol] = useState("");
  const [strategySymbol, setStrategySymbol] = useState("");
  const [favoriteMessage, setFavoriteMessage] = useState<string | null>(null);
  const [strategyMessage, setStrategyMessage] = useState<string | null>(null);
  const [lastFavoriteAction, setLastFavoriteAction] = useState<{ action: "add" | "remove"; symbol: string } | null>(null);
  const [lastStrategyAction, setLastStrategyAction] = useState<{ action: "add" | "remove"; symbol: string } | null>(null);

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
      setManualActionMessage("구독 상태가 변경되었습니다.");
      void queryClient.invalidateQueries({ queryKey: ["kis"] });
    },
    onError: (error) => {
      setManualActionMessage(error instanceof Error ? `요청 실패: ${error.message}` : "요청 실패");
    }
  });

  const { data: favorites = { items: [] as string[] }, isLoading: isFavoritesLoading, refetch: refetchFavorites } = useQuery({
    queryKey: ["subscriptions", "favorites", mode, channel],
    queryFn: () =>
      fetchJson<{ mode: Mode; channel: KisChannel; items: string[]; returnedCount: number; status: string }>(
        `/api/subscriptions/favorites?mode=${mode}&channel=${channel}`
      )
  });

  const { data: strategySymbols = { items: [] as string[] }, isLoading: isStrategyLoading, refetch: refetchStrategy } = useQuery({
    queryKey: ["subscriptions", "strategy", mode],
    queryFn: () =>
      fetchJson<{ mode: Mode; items: string[]; returnedCount: number; status: string }>(`/api/subscriptions/strategy-symbols?mode=${mode}`)
  });

  const {
    data: routingStatus,
    isLoading: isRoutingStatusLoading,
    refetch: refetchRoutingStatus
  } = useQuery({
    queryKey: ["subscriptions", "routing-status", mode],
    queryFn: () =>
      fetchJson<{
        generatedAt: string;
        mode: Mode;
        ws: { slotUsed: number; slotMax: number; symbols: string[] };
        rest: { symbols: string[] };
        sources: { manual: string[]; favorites: string[]; strategyPriority: string[] };
        status: string;
      }>(`/api/subscriptions/routing-status?mode=${mode}`)
  });

  const favoritesMutation = useMutation({
    mutationFn: ({ action, targetSymbol }: { action: "add" | "remove"; targetSymbol: string }) =>
      fetchJson<{ status: string; totalSelected: number }>("/api/subscriptions/favorites", {
        method: action === "add" ? "POST" : "DELETE",
        body: JSON.stringify({ mode, channel, symbol: targetSymbol.trim() })
      }),
    onSuccess: () => {
      setFavoriteMessage("즐겨찾기 라우팅이 변경되었습니다.");
      void refetchFavorites();
      void refetchRoutingStatus();
    },
    onError: (error) => {
      setFavoriteMessage(error instanceof Error ? `요청 실패: ${error.message}` : "요청 실패");
    }
  });

  const strategyMutation = useMutation({
    mutationFn: ({ action, targetSymbol }: { action: "add" | "remove"; targetSymbol: string }) =>
      fetchJson<{ status: string; totalSelected: number }>("/api/subscriptions/strategy-symbols", {
        method: action === "add" ? "POST" : "DELETE",
        body: JSON.stringify({ mode, symbol: targetSymbol.trim() })
      }),
    onSuccess: () => {
      setStrategyMessage("전략 우선 라우팅이 변경되었습니다.");
      void refetchStrategy();
      void refetchRoutingStatus();
    },
    onError: (error) => {
      setStrategyMessage(error instanceof Error ? `요청 실패: ${error.message}` : "요청 실패");
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
    isFavoritesLoading ||
    isStrategyLoading ||
    isRoutingStatusLoading ||
    subscriptionMutation.isPending ||
    favoritesMutation.isPending ||
    strategyMutation.isPending ||
    isPriceLoading;
  
  const getStatusMessage = () => {
    if (manualActionMessage) return manualActionMessage;
    if (isPriceLoading) return "현재가 조회 중...";
    return "시스템 준비 완료";
  };

  const handleSubscriptionAction = (action: "add" | "remove", retryPayload?: { symbol: string }) => {
    const targetSymbol = (retryPayload?.symbol ?? symbol).trim().toUpperCase();
    if (!targetSymbol) {
      setManualActionMessage("종목코드를 입력해 주세요.");
      return;
    }
    if (subscriptionMutation.isPending) {
      return;
    }
    setLastAction({ action, symbol: targetSymbol });
    subscriptionMutation.mutate({ action, targetSymbol });
  };

  const handleFavoritesAction = (action: "add" | "remove", retryPayload?: { symbol: string }) => {
    const targetSymbol = (retryPayload?.symbol ?? favoriteSymbol).trim().toUpperCase();
    if (!targetSymbol) {
      setFavoriteMessage("종목코드를 입력해 주세요.");
      return;
    }
    if (favoritesMutation.isPending) return;
    setLastFavoriteAction({ action, symbol: targetSymbol });
    favoritesMutation.mutate({ action, targetSymbol });
  };

  const handleStrategyAction = (action: "add" | "remove", retryPayload?: { symbol: string }) => {
    const targetSymbol = (retryPayload?.symbol ?? strategySymbol).trim().toUpperCase();
    if (!targetSymbol) {
      setStrategyMessage("종목코드를 입력해 주세요.");
      return;
    }
    if (strategyMutation.isPending) return;
    setLastStrategyAction({ action, symbol: targetSymbol });
    strategyMutation.mutate({ action, targetSymbol });
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

        <SectionCard title="KIS 구독 라우팅 제어 (Manual)" icon={Activity}>
          <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <div className="form-row">
              <input value={symbol} onChange={(e) => setSymbol(e.target.value.toUpperCase())} placeholder="종목코드" style={{ width: "150px" }} />
              <button className="btn btn-primary" disabled={subscriptionMutation.isPending} onClick={() => handleSubscriptionAction("add")}>
                구독 추가
              </button>
              <button className="btn btn-danger" disabled={subscriptionMutation.isPending} onClick={() => handleSubscriptionAction("remove")}>
                구독 해지
              </button>
              {subscriptionMutation.isError && lastAction && (
                <button
                  className="btn btn-outline"
                  disabled={subscriptionMutation.isPending}
                  onClick={() => handleSubscriptionAction(lastAction.action, { symbol: lastAction.symbol })}
                >
                  실패한 요청 재시도
                </button>
              )}
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

        <SectionCard title="즐겨찾기 라우팅 (Favorites)" icon={Activity}>
          <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <div className="form-row">
              <input value={favoriteSymbol} onChange={(e) => setFavoriteSymbol(e.target.value.toUpperCase())} placeholder="종목코드" style={{ width: "150px" }} />
              <button className="btn btn-primary" disabled={favoritesMutation.isPending} onClick={() => handleFavoritesAction("add")}>
                즐겨찾기 추가
              </button>
              <button className="btn btn-danger" disabled={favoritesMutation.isPending} onClick={() => handleFavoritesAction("remove")}>
                즐겨찾기 해지
              </button>
              {favoritesMutation.isError && lastFavoriteAction && (
                <button
                  className="btn btn-outline"
                  disabled={favoritesMutation.isPending}
                  onClick={() => handleFavoritesAction(lastFavoriteAction.action, { symbol: lastFavoriteAction.symbol })}
                >
                  실패한 요청 재시도
                </button>
              )}
            </div>
            <div className="meta-row" style={{ padding: 0 }}>
              <span>즐겨찾기 종목 수: {favorites.items?.length ?? 0}건</span>
              <span>{favoriteMessage ?? "즐겨찾기 라우팅 대기 중"}</span>
            </div>
          </div>
        </SectionCard>
      </div>

      <div className="feature-grid">
        <SectionCard title="전략 우선 라우팅 (Strategy Priority)" icon={Activity}>
          <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <div className="form-row">
              <input value={strategySymbol} onChange={(e) => setStrategySymbol(e.target.value.toUpperCase())} placeholder="종목코드" style={{ width: "150px" }} />
              <button className="btn btn-primary" disabled={strategyMutation.isPending} onClick={() => handleStrategyAction("add")}>
                전략 종목 추가
              </button>
              <button className="btn btn-danger" disabled={strategyMutation.isPending} onClick={() => handleStrategyAction("remove")}>
                전략 종목 해지
              </button>
              {strategyMutation.isError && lastStrategyAction && (
                <button
                  className="btn btn-outline"
                  disabled={strategyMutation.isPending}
                  onClick={() => handleStrategyAction(lastStrategyAction.action, { symbol: lastStrategyAction.symbol })}
                >
                  실패한 요청 재시도
                </button>
              )}
            </div>
            <div className="meta-row" style={{ padding: 0 }}>
              <span>전략 우선 종목 수: {strategySymbols.items?.length ?? 0}건</span>
              <span>{strategyMessage ?? "전략 우선 라우팅 대기 중"}</span>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="라우팅 상태 검증 (Routing Status)" icon={RefreshCw}>
          <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "12px" }}>
            <div className="form-row">
              <button className="btn btn-outline" onClick={() => void refetchRoutingStatus()}>
                상태 새로고침
              </button>
            </div>
            <div className="meta-row" style={{ padding: 0 }}>
              <span>생성 시각: {routingStatus?.generatedAt ?? "-"}</span>
              <span>WS 슬롯: {routingStatus ? `${routingStatus.ws.slotUsed}/${routingStatus.ws.slotMax}` : "-"}</span>
              <span>WS 종목: {routingStatus?.ws.symbols.length ?? 0}건</span>
              <span>REST 종목: {routingStatus?.rest.symbols.length ?? 0}건</span>
            </div>
            <div className="meta-row" style={{ padding: 0 }}>
              <span>수동 출처: {routingStatus?.sources?.manual?.length ?? 0}건</span>
              <span>즐겨찾기 출처: {routingStatus?.sources?.favorites?.length ?? 0}건</span>
              <span>전략 출처: {routingStatus?.sources?.strategyPriority?.length ?? 0}건</span>
            </div>
          </div>
        </SectionCard>

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
