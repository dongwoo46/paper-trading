import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, DollarSign, RefreshCw, Search } from "lucide-react";
import type { CatalogResponse, KrSymbol, Mode } from "../../../entities/symbol/model/types";
import { fetchJson, normalizeByModes, type ModeSubscriptions } from "../../../shared/api";
import { fetchSubscriptionStatus } from "../../../shared/api/subscriptionStatusApi";
import { SectionCard, StatusBar } from "../../../shared/ui";
import { KisModeList } from "./KisModeList";
import { KisSearchList } from "./KisSearchList";
import { Alert, AlertDescription } from "@/shared/ui/shadcn/alert";
import { Badge } from "@/shared/ui/shadcn/badge";
import { Button } from "@/shared/ui/shadcn/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/shadcn/card";
import { Input } from "@/shared/ui/shadcn/input";
import { NativeSelect, NativeSelectOption } from "@/shared/ui/shadcn/native-select";
import { Skeleton } from "@/shared/ui/shadcn/skeleton";

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
        <Card size="sm"><CardHeader><CardTitle className="text-muted-foreground">계좌 모드</CardTitle></CardHeader><CardContent className="text-xl font-semibold text-primary">{mode === "paper" ? "모의투자" : "실전투자"}</CardContent></Card>
        <Card size="sm"><CardHeader><CardTitle className="text-muted-foreground">수집 채널</CardTitle></CardHeader><CardContent className="text-xl font-semibold text-primary">{channel === "ws" ? "실시간 (WS)" : "일반 (REST)"}</CardContent></Card>
        <Card size="sm"><CardHeader><CardTitle className="text-muted-foreground">현재 구독 수</CardTitle></CardHeader><CardContent className="text-xl font-semibold text-primary">{modeSubscriptions.items?.length ?? 0} 건</CardContent></Card>
      </div>

      <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
        <SectionCard
          title="한국투자증권(KIS) 종목 카탈로그"
          icon={Search}
          headerAction={(
            <div className="flex gap-3 flex-wrap">
              <NativeSelect className="w-30" aria-label="계좌 모드" value={mode} onChange={(e) => setMode(e.target.value as Mode)}><NativeSelectOption value="paper">모의투자</NativeSelectOption><NativeSelectOption value="live">실전투자</NativeSelectOption></NativeSelect>
              <NativeSelect className="w-25" aria-label="수집 채널" value={channel} onChange={(e) => setChannel(e.target.value as KisChannel)}><NativeSelectOption value="ws">WS</NativeSelectOption><NativeSelectOption value="rest">REST</NativeSelectOption></NativeSelect>
              <Input className="w-45" aria-label="종목 검색" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="종목코드 또는 이름" />
              <NativeSelect className="w-30" aria-label="시장" value={market} onChange={(e) => setMarket(e.target.value)}><NativeSelectOption value="">전체 시장</NativeSelectOption><NativeSelectOption value="KOSPI">KOSPI</NativeSelectOption><NativeSelectOption value="KOSDAQ">KOSDAQ</NativeSelectOption></NativeSelect>
              <NativeSelect className="w-35" aria-label="구독 상태" value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}><NativeSelectOption value="">전체 상태</NativeSelectOption><NativeSelectOption value="subscribed">구독 중</NativeSelectOption><NativeSelectOption value="unsubscribed">미구독</NativeSelectOption></NativeSelect>
            </div>
          )}
        >
          <div className="flex gap-2.5 flex-wrap px-6 pb-3.5">
            <Badge variant="secondary">전체 카탈로그: {catalog.totalCatalogCount}</Badge>
            <Badge variant="secondary">전체 구독 중: {catalog.totalSubscribedCount}</Badge>
            <Badge variant="secondary">조회 결과: {catalog.returnedCount}</Badge>
          </div>
          <KisSearchList
            results={catalog.items}
            onSelect={(selectedSymbol) => {
              setSymbol(selectedSymbol);
            }}
          />
          {catalog.items.length < catalog.totalCatalogCount && (
            <Button
              variant="ghost"
              className="mx-6 my-2 w-fit"
              onClick={() => setCatalogLimit(prev => prev + 20)}
            >
              <RefreshCw size={14} /> 더보기 ({catalog.items.length} / {catalog.totalCatalogCount})
            </Button>
          )}
        </SectionCard>

        <SectionCard title="KIS 구독 제어" icon={Activity}>
          <div className="px-6 py-4 flex flex-col gap-3">
            <div className="flex gap-3 flex-wrap">
              <Input
                className="w-38"
                value={symbol}
                onChange={(e) => setSymbol(e.target.value.toUpperCase())}
                placeholder="종목코드"
              />
              <Button onClick={() => symbol && subscriptionMutation.mutate({ action: "add", targetSymbol: symbol })}>
                구독 추가
              </Button>
              <Button variant="destructive" onClick={() => symbol && subscriptionMutation.mutate({ action: "remove", targetSymbol: symbol })}>
                구독 해지
              </Button>
            </div>
            <div className="flex gap-2.5 flex-wrap">
              <Badge variant="secondary">선택된 종목: {symbol || "-"}</Badge>
              <Badge variant="secondary">구독 중인 종목: {selectedSet.size}건</Badge>
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
              <Input
                className="w-38"
                value={priceSymbol}
                onChange={(e) => setPriceSymbol(e.target.value.toUpperCase())}
                placeholder="종목코드"
              />
              <Button variant="outline" onClick={() => void refetchPrice()}>
                현재가 조회
              </Button>
            </div>
            <div className="flex gap-2.5 flex-wrap">
              <Badge variant="secondary">조회 결과 로드: {priceResult ? "성공" : "없음"}</Badge>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="레거시 API 연결 상태 (WS/REST 목록)" icon={Activity}>
          <div className="px-6 py-4 flex flex-col gap-2.5">
            <div className="flex gap-2.5 flex-wrap">
              <Badge variant="secondary">모의투자 구독: {oldModeSubscriptions.paper.length}건</Badge>
              <Badge variant="secondary">실전투자 구독: {oldModeSubscriptions.live.length}건</Badge>
              <Badge variant="secondary">엔드포인트: {oldPath}</Badge>
            </div>
          </div>
        </SectionCard>
      </div>

      <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
        <SectionCard
          title="구독 상태 모니터링 (읽기 전용)"
          icon={Activity}
          headerAction={(
            <Button variant="outline" onClick={() => void refetchSubscriptionStatus()}>
              새로고침
            </Button>
          )}
        >
          <div className="px-6 py-4 flex flex-col gap-3">
            {isSubscriptionStatusLoading && <Skeleton className="h-24 w-full" aria-label="구독 상태 로딩 중" />}
            {isSubscriptionStatusError && (
              <Alert variant="destructive"><AlertDescription>상태 조회 실패: {subscriptionStatusError instanceof Error ? subscriptionStatusError.message : "알 수 없는 오류"}</AlertDescription></Alert>
            )}
            {!isSubscriptionStatusLoading && !isSubscriptionStatusError && subscriptionStatus && (
              <>
                <div className="flex gap-2.5 flex-wrap">
                  <Badge variant="secondary">생성 시각: {subscriptionStatus.generatedAt}</Badge>
                  <Badge variant="secondary">전역 WS 슬롯: {subscriptionStatus.totalWsSlotUsed} / {subscriptionStatus.totalWsSlotMax}</Badge>
                </div>
                {subscriptionStatus.modes.length === 0 && <div>모드 상태 데이터가 없습니다.</div>}
                {subscriptionStatus.modes.map((item) => (
                  <Card key={item.mode} size="sm" className="gap-2 p-3">
                    <div className="flex gap-2.5 flex-wrap">
                      <strong>{item.mode}</strong>
                      <Badge variant="secondary">{item.connectionStatus}</Badge>
                      <Badge variant="secondary">마지막 연결: {item.lastConnectedAt ?? "-"}</Badge>
                      <Badge variant="secondary">재연결 횟수: {item.reconnectAttempts}</Badge>
                      <Badge variant="secondary">WS 슬롯: {item.wsSlotUsed} / {item.wsSlotMax}</Badge>
                    </div>
                    <div className="flex gap-2.5 flex-wrap">
                      <Badge variant="secondary">WS 심볼({item.wsSymbols.length}): {item.wsSymbols.join(", ") || "-"}</Badge>
                    </div>
                    <div className="flex gap-2.5 flex-wrap">
                      <Badge variant="secondary">REST 심볼({item.restSymbols.length}): {item.restSymbols.join(", ") || "-"}</Badge>
                    </div>
                  </Card>
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
