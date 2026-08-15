import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, Database, RefreshCw, Search } from "lucide-react";
import type { CatalogResponse, OhlcvDailyBar, OhlcvSymbolSummary, SymbolCatalogItem } from "../../../entities/symbol/model/types";
import { fetchJson } from "../../../shared/api";
import { SectionCard, StatusBar } from "../../../shared/ui";
import { CatalogSelectionList } from "./CatalogSelectionList";
import { CatalogTable } from "./CatalogTable";
import { Badge } from "@/shared/ui/shadcn/badge";
import { Button } from "@/shared/ui/shadcn/button";
import { Checkbox } from "@/shared/ui/shadcn/checkbox";
import { Input } from "@/shared/ui/shadcn/input";
import { Label } from "@/shared/ui/shadcn/label";
import { NativeSelect, NativeSelectOption } from "@/shared/ui/shadcn/native-select";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/shadcn/tabs";

type StatusFilter = "" | "SUBSCRIBED" | "UNSUBSCRIBED";

const EMPTY: CatalogResponse<SymbolCatalogItem> = {
  items: [],
  returnedCount: 0,
  totalCatalogCount: 0,
  totalSubscribedCount: 0
};

export function SymbolCatalogPanel({ isPykrx }: { isPykrx: boolean; title: string }) {
  const queryClient = useQueryClient();
  const source = isPykrx ? "pykrx" : "yfinance";
  const keyName = isPykrx ? "symbol" : "ticker";
  const catalogPath = isPykrx ? "/api/pykrx/symbols/catalog" : "/api/yfinance/symbols/catalog";
  const searchPath = isPykrx ? "/api/pykrx/symbols/search" : "/api/yfinance/symbols/search";
  const subscriptionsPath = isPykrx ? "/api/pykrx/symbols/subscriptions" : "/api/yfinance/symbols/subscriptions";
  const syncPath = isPykrx ? "/api/pykrx/symbols/catalog/sync" : "";
  const collectionStatusPath = isPykrx ? "/api/pykrx/symbols/collection-status" : "/api/yfinance/symbols/collection-status";
  const marketOptions = isPykrx ? ["KOSPI", "KOSDAQ", "ETF"] : ["US", "INDEX", "ETF"];

  const [activeTab, setActiveTab] = useState<"catalog" | "operations" | "ohlcv">("catalog");
  const [query, setQuery] = useState("");
  const [market, setMarket] = useState("");
  const [status, setStatus] = useState<StatusFilter>("");
  const [isSearching, setIsSearching] = useState(false);
  const [catalogLimit, setCatalogLimit] = useState(20);
  const [targetId, setTargetId] = useState("");
  const [fetchedUntilDate, setFetchedUntilDate] = useState("");
  const [ohlcvSymbol, setOhlcvSymbol] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  const [collectProvider, setCollectProvider] = useState<"all" | "yfinance" | "pykrx">("all");
  const [collectStart, setCollectStart] = useState("2010-01-01");
  const [collectEnd, setCollectEnd] = useState(new Date().toISOString().split("T")[0]);
  const [isOnlyDefault, setIsOnlyDefault] = useState(false);
  const [collectSummary, setCollectSummary] = useState<string | null>(null);

  const { data: catalog = EMPTY, isLoading: isCatalogLoading } = useQuery({
    queryKey: [source, "catalog", market, status, isSearching ? query : "", catalogLimit],
    queryFn: () => {
      const params = new URLSearchParams({ limit: catalogLimit.toString() });
      if (isSearching && query.trim()) params.set("query", query.trim());
      if (market) params.set("market", market);
      if (status) params.set("status", status);
      const path = isSearching ? searchPath : catalogPath;
      return fetchJson<CatalogResponse<SymbolCatalogItem>>(`${path}?${params.toString()}`);
    }
  });

  const { data: subscribedRows = [], isLoading: isSubscriptionsLoading } = useQuery({
    queryKey: [source, "subscriptions"],
    queryFn: () => fetchJson<SymbolCatalogItem[]>(subscriptionsPath)
  });

  const { data: ohlcvSymbols = [] } = useQuery({
    queryKey: [source, "ohlcv", "symbols"],
    queryFn: () => fetchJson<OhlcvSymbolSummary[]>(`/api/${source}/ohlcv/symbols?limit=100`)
  });

  const ohlcvQueryString = useMemo(() => {
    const params = new URLSearchParams({ symbol: ohlcvSymbol.trim() });
    if (fromDate) params.set("from", fromDate);
    if (toDate) params.set("to", toDate);
    return params.toString();
  }, [fromDate, ohlcvSymbol, toDate]);

  const { data: dailyBars = [], isFetching: isDailyLoading, refetch: refetchDaily } = useQuery({
    queryKey: [source, "ohlcv", "daily", ohlcvQueryString],
    queryFn: () => fetchJson<OhlcvDailyBar[]>(`/api/${source}/ohlcv/daily?${ohlcvQueryString}`),
    enabled: false
  });

  const subscriptionMutation = useMutation({
    mutationFn: ({ id, method }: { id: string; method: "POST" | "DELETE" }) => {
      const payload = JSON.stringify({ [keyName]: id });
      return fetchJson<{ status: string }>(subscriptionsPath, { method, body: payload });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [source, "catalog"] });
      void queryClient.invalidateQueries({ queryKey: [source, "subscriptions"] });
    }
  });

  const syncMutation = useMutation({
    mutationFn: () => {
      if (!syncPath) return Promise.resolve({ status: "unsupported" });
      return fetchJson<{ status: string; affectedRows?: number }>(syncPath, { method: "POST" });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [source, "catalog"] });
    }
  });

  const collectionStatusMutation = useMutation({
    mutationFn: () => {
      const id = targetId.trim();
      if (!id || !fetchedUntilDate) {
        throw new Error("심볼과 수집 기준일자가 필요합니다.");
      }
      const payload = isPykrx
        ? { symbol: id, fetchedUntilDate }
        : { ticker: id.toUpperCase(), fetchedUntilDate };
      return fetchJson<{ status: string }>(collectionStatusPath, {
        method: "POST",
        body: JSON.stringify(payload)
      });
    }
  });

  const collectMutation = useMutation({
    mutationFn: () => {
      return fetchJson<{
        provider: string;
        symbols: number;
        success_symbols: number;
        failed_symbols: number;
        summary_path: string;
      }>("/api/collect/daily", {
        method: "POST",
        body: JSON.stringify({
          provider: collectProvider,
          start: collectStart,
          end: collectEnd,
          output_root: "data",
          only_default: isOnlyDefault,
          auto_adjust: false,
          adjusted: false
        })
      });
    },
    onSuccess: (data) => {
      setCollectSummary(`완료 (성공: ${data.success_symbols}, 실패: ${data.failed_symbols})`);
      void queryClient.invalidateQueries({ queryKey: [source, "catalog"] });
    },
    onError: () => {
      setCollectSummary("수집 요청 중 오류 발생");
    }
  });

  const isSubscribed = (id: string) => {
    return subscribedRows.some((row) => (row.symbol ?? row.ticker) === id);
  };

  const handleToggle = (id: string) => {
    subscriptionMutation.mutate({ id, method: isSubscribed(id) ? "DELETE" : "POST" });
  };

  const loading = isCatalogLoading || isSubscriptionsLoading || subscriptionMutation.isPending || syncMutation.isPending || collectionStatusMutation.isPending || collectMutation.isPending || isDailyLoading;

  const getStatusMessage = () => {
    if (collectSummary) return collectSummary;
    if (subscriptionMutation.isError || syncMutation.isError || collectionStatusMutation.isError || collectMutation.isError) return "요청 실패";
    if (subscriptionMutation.isSuccess) return "구독 상태가 업데이트되었습니다.";
    if (syncMutation.isSuccess) return "카탈로그 동기화 완료";
    if (collectionStatusMutation.isSuccess) return "수집 상태 업데이트 완료";
    return "준비 완료";
  };

  return (
    <div className="flex flex-col gap-2.5 mt-5">
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as typeof activeTab)} className="mb-5">
        <TabsList className="h-auto flex-wrap">
          <TabsTrigger value="catalog" className="px-4 py-2">종목 탐색</TabsTrigger>
          <TabsTrigger value="operations" className="px-4 py-2">데이터 수집 관리</TabsTrigger>
          <TabsTrigger value="ohlcv" className="px-4 py-2">시세 데이터 조회</TabsTrigger>
        </TabsList>
      </Tabs>

      {activeTab === "catalog" && (
        <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
          <SectionCard
            title={isPykrx ? "국내 주식 카탈로그 (pykrx)" : "해외 주식 카탈로그 (yfinance)"}
            icon={Database}
            headerAction={(
              <div className="flex gap-3 flex-wrap">
                <Input
                  className="w-55"
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    if (!e.target.value.trim()) setIsSearching(false);
                  }}
                  onKeyDown={(e) => e.key === "Enter" && setIsSearching(true)}
                  placeholder="심볼 또는 종목명 검색"
                />
                <NativeSelect className="w-35" aria-label="시장" value={market} onChange={(e) => setMarket(e.target.value)}>
                  <NativeSelectOption value="">전체 시장</NativeSelectOption>
                  {marketOptions.map((m) => (
                    <NativeSelectOption key={m} value={m}>{m}</NativeSelectOption>
                  ))}
                </NativeSelect>
                <NativeSelect className="w-38" aria-label="구독 상태" value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}>
                  <NativeSelectOption value="">전체 상태</NativeSelectOption>
                  <NativeSelectOption value="SUBSCRIBED">구독 중</NativeSelectOption>
                  <NativeSelectOption value="UNSUBSCRIBED">미구독</NativeSelectOption>
                </NativeSelect>
                <Button variant="outline" size="icon" onClick={() => setIsSearching(true)} aria-label="종목 검색">
                  <Search size={14} />
                </Button>
              </div>
            )}
          >
            <div className="flex gap-2.5 flex-wrap px-6 pb-3.5">
              <Badge variant="secondary">전체 카탈로그: {catalog.totalCatalogCount}</Badge>
              <Badge variant="secondary">전체 구독 중: {catalog.totalSubscribedCount}</Badge>
              <Badge variant="secondary">조회 결과: {catalog.returnedCount}</Badge>
            </div>
            <CatalogTable
              rows={catalog.items.map((item) => {
                const id = item.symbol ?? item.ticker ?? "";
                return { ...item, enabled: isSubscribed(id) };
              })}
              onToggle={(id) => handleToggle(id)}
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

          <SectionCard title={`활성 구독 목록 (${subscribedRows.length})`} icon={Activity}>
            <CatalogSelectionList items={subscribedRows} onRemove={(id) => handleToggle(id)} />
          </SectionCard>
        </div>
      )}

      {activeTab === "operations" && (
        <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
          <SectionCard title="데이터 소스 관리" icon={RefreshCw}>
            <div className="px-6 py-4 flex flex-col gap-3">
              <p className="text-sm text-muted-foreground">
                시장에서 유효한 종목 목록을 동기화하거나 특정 종목의 과거 데이터 수집 범위를 조정합니다.
              </p>
              {isPykrx && (
                <Button variant="outline" className="w-fit" onClick={() => syncMutation.mutate()}>
                  전체 카탈로그 수동 동기화
                </Button>
              )}
              <div className="flex gap-3 flex-wrap mt-3">
                <Input
                  className="w-55"
                  value={targetId}
                  onChange={(e) => setTargetId(e.target.value)}
                  placeholder={isPykrx ? "심볼 (예: 005930)" : "티커 (예: AAPL)"}
                />
                <Input type="date" className="w-45" aria-label="수집 기준일" value={fetchedUntilDate} onChange={(e) => setFetchedUntilDate(e.target.value)} />
                <Button onClick={() => collectionStatusMutation.mutate()}>
                  수집 기준일 업데이트
                </Button>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="전체 시세 데이터 동기화 (Batch)" icon={RefreshCw}>
            <div className="px-6 py-4 flex flex-col gap-3">
              <p className="text-sm text-muted-foreground">
                구독된 모든 종목의 일봉 데이터를 일괄 수집합니다. (마지막 수집일 이후 증분 수집)
              </p>
              <div className="flex gap-3 flex-wrap">
                <NativeSelect className="w-32" aria-label="수집 소스" value={collectProvider} onChange={(e) => setCollectProvider(e.target.value as "all" | "yfinance" | "pykrx")}>
                  <NativeSelectOption value="all">모든 소스</NativeSelectOption>
                  <NativeSelectOption value="pykrx">국내 주식</NativeSelectOption>
                  <NativeSelectOption value="yfinance">해외 주식</NativeSelectOption>
                </NativeSelect>
                <div className="flex items-center gap-2">
                  <Checkbox id="only-default-symbols" checked={isOnlyDefault} onCheckedChange={setIsOnlyDefault} />
                  <Label htmlFor="only-default-symbols">기본 종목만</Label>
                </div>
              </div>
              <div className="flex gap-3 flex-wrap">
                <Label htmlFor="collect-start" className="self-center">시작</Label>
                <Input id="collect-start" type="date" className="w-40" value={collectStart} onChange={(e) => setCollectStart(e.target.value)} />
                <Label htmlFor="collect-end" className="self-center">종료</Label>
                <Input id="collect-end" type="date" className="w-40" value={collectEnd} onChange={(e) => setCollectEnd(e.target.value)} />
                <Button onClick={() => { setCollectSummary(null); collectMutation.mutate(); }}>
                  데이터 수집 시작
                </Button>
              </div>
            </div>
          </SectionCard>
        </div>
      )}

      {activeTab === "ohlcv" && (
        <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
          <SectionCard title="과거 시세 조회 (OHLCV)" icon={Database}>
            <div className="px-6 py-4 flex flex-col gap-3">
              <div className="flex gap-2.5 flex-wrap">
                <Badge variant="secondary">데이터 수집 상태 통합 확인</Badge>
                <Badge variant="secondary">DB 로드 가능 종목: {ohlcvSymbols.length}</Badge>
              </div>
              <div className="flex gap-3 flex-wrap">
                <Input className="w-40" aria-label="OHLCV 심볼" value={ohlcvSymbol} onChange={(e) => setOhlcvSymbol(e.target.value.toUpperCase())} placeholder="심볼" />
                <Input type="date" className="w-40" aria-label="OHLCV 시작일" value={fromDate} onChange={(e) => setFromDate(e.target.value)} />
                <Input type="date" className="w-40" aria-label="OHLCV 종료일" value={toDate} onChange={(e) => setToDate(e.target.value)} />
                <Button onClick={() => void refetchDaily()}>
                  시세 데이터 로드
                </Button>
              </div>
              <div className="flex gap-2.5 flex-wrap">
                <Badge variant="secondary">조회 결과: {dailyBars.length}건</Badge>
              </div>
            </div>
          </SectionCard>
        </div>
      )}

      <StatusBar message={getStatusMessage()} loading={loading} />
    </div>
  );
}
