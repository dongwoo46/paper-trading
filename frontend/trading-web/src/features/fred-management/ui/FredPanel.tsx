import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, Database, RefreshCw, Search, Trash2 } from "lucide-react";
import type { CatalogResponse, FredCatalogItem } from "../../../entities/symbol/model/types";
import { fetchJson } from "../../../shared/api";
import { Chip, SectionCard, StatusBar } from "../../../shared/ui";
import { Badge } from "@/shared/ui/shadcn/badge";
import { Button } from "@/shared/ui/shadcn/button";
import { Input } from "@/shared/ui/shadcn/input";
import { NativeSelect, NativeSelectOption } from "@/shared/ui/shadcn/native-select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/ui/shadcn/table";
import { Tabs, TabsList, TabsTrigger } from "@/shared/ui/shadcn/tabs";

type StatusFilter = "" | "SUBSCRIBED" | "UNSUBSCRIBED";

const EMPTY: CatalogResponse<FredCatalogItem> = {
  items: [],
  returnedCount: 0,
  totalCatalogCount: 0,
  totalSubscribedCount: 0
};

export function FredPanel() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<"catalog" | "sync">("catalog");
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("");
  const [frequency, setFrequency] = useState("");
  const [status, setStatus] = useState<StatusFilter>("");
  const [isSearching, setIsSearching] = useState(false);
  const [externalQuery, setExternalQuery] = useState("");
  const [seriesId, setSeriesId] = useState("");
  const [maxCategories, setMaxCategories] = useState("500");
  const [pageSize, setPageSize] = useState("100");
  const [catalogLimit, setCatalogLimit] = useState(20);

  const { data: catalog = EMPTY, isLoading: isCatalogLoading } = useQuery({
    queryKey: ["fred", "catalog", query, category, frequency, status, isSearching, catalogLimit],
    queryFn: () => {
      const params = new URLSearchParams({ limit: catalogLimit.toString() });
      if (isSearching && query.trim()) params.set("query", query.trim());
      if (category.trim()) params.set("category", category.trim());
      if (frequency) params.set("frequency", frequency);
      if (status) params.set("status", status);
      return fetchJson<CatalogResponse<FredCatalogItem>>(`/api/fred/series/catalog?${params.toString()}`);
    }
  });

  const { data: subscriptions = [], isLoading: isSubscriptionsLoading } = useQuery({
    queryKey: ["fred", "subscriptions"],
    queryFn: () => fetchJson<FredCatalogItem[]>("/api/fred/series/subscriptions")
  });

  const { data: externalSearchResult, refetch: refetchExternalSearch, isFetching: isExternalSearchLoading } = useQuery({
    queryKey: ["fred", "external-search", externalQuery],
    queryFn: () => fetchJson<{ seriess?: Array<{ id: string; title: string }> }>(`/api/fred/series/search?query=${encodeURIComponent(externalQuery)}&limit=30`),
    enabled: false
  });

  const { data: infoResult, refetch: refetchInfo, isFetching: isInfoLoading } = useQuery({
    queryKey: ["fred", "info", seriesId],
    queryFn: () => fetchJson<Record<string, unknown>>(`/api/fred/series/info?seriesId=${encodeURIComponent(seriesId)}&observationLimit=30`),
    enabled: false
  });

  const { data: observationsResult, refetch: refetchObservations, isFetching: isObservationsLoading } = useQuery({
    queryKey: ["fred", "observations", seriesId],
    queryFn: () => fetchJson<{ observations?: Array<unknown> }>(`/api/fred/series/observations?seriesId=${encodeURIComponent(seriesId)}&limit=100`),
    enabled: false
  });

  const subscriptionMutation = useMutation({
    mutationFn: ({ method, selectedSeriesId }: { method: "POST" | "DELETE"; selectedSeriesId: string }) =>
      fetchJson("/api/fred/series/subscriptions", {
        method,
        body: JSON.stringify({ seriesId: selectedSeriesId })
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["fred"] });
    }
  });

  const syncMutation = useMutation({
    mutationFn: () => {
      const params = new URLSearchParams({
        maxCategories: maxCategories || "500",
        pageSize: pageSize || "100"
      });
      return fetchJson<{ status: string; processedCategories: number; upsertedSeries: number }>(
        `/api/fred/series/catalog/sync?${params.toString()}`,
        { method: "POST" }
      );
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["fred", "catalog"] });
    }
  });

  const isSubscribed = (selectedSeriesId: string) => subscriptions.some((row) => row.seriesId === selectedSeriesId);
  const searchSeriesCount = Array.isArray(externalSearchResult?.seriess) ? externalSearchResult.seriess.length : 0;
  const observationCount = Array.isArray(observationsResult?.observations) ? observationsResult.observations.length : 0;
  const loading = isCatalogLoading || isSubscriptionsLoading || isExternalSearchLoading || isInfoLoading || isObservationsLoading || subscriptionMutation.isPending || syncMutation.isPending;

  const getStatusMessage = () => {
    if (subscriptionMutation.isError || syncMutation.isError) return "요청 실패";
    if (subscriptionMutation.isSuccess) return "구독 정보가 변경되었습니다.";
    if (syncMutation.isSuccess) return "카탈로그 동기화 완료";
    return "준비 완료";
  };

  return (
    <div className="flex flex-col gap-2.5 mt-5">
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as typeof activeTab)} className="mb-5">
        <TabsList className="h-auto flex-wrap">
          <TabsTrigger value="catalog" className="px-4 py-2">경제지표 탐색</TabsTrigger>
          <TabsTrigger value="sync" className="px-4 py-2">데이터 연동 및 품질 관리</TabsTrigger>
        </TabsList>
      </Tabs>

      {activeTab === "catalog" && (
        <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
          <SectionCard
            title="FRED 경제지표 카탈로그"
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
                  placeholder="시리즈 ID 또는 지표명"
                />
                <Input className="w-30" aria-label="카테고리" value={category} onChange={(e) => setCategory(e.target.value)} placeholder="카테고리" />
                <NativeSelect className="w-32" aria-label="주기" value={frequency} onChange={(e) => setFrequency(e.target.value)}>
                  <NativeSelectOption value="">전체 주기</NativeSelectOption><NativeSelectOption value="D">일간 (Daily)</NativeSelectOption><NativeSelectOption value="W">주간 (Weekly)</NativeSelectOption><NativeSelectOption value="M">월간 (Monthly)</NativeSelectOption><NativeSelectOption value="Q">분기 (Quarterly)</NativeSelectOption><NativeSelectOption value="A">연간 (Annual)</NativeSelectOption>
                </NativeSelect>
                <NativeSelect className="w-32" aria-label="구독 상태" value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}>
                  <NativeSelectOption value="">전체 상태</NativeSelectOption><NativeSelectOption value="SUBSCRIBED">구독 중</NativeSelectOption><NativeSelectOption value="UNSUBSCRIBED">미구독</NativeSelectOption>
                </NativeSelect>
                <Button variant="outline" size="icon" onClick={() => setIsSearching(true)} aria-label="경제지표 검색">
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
            <div className="min-h-75 flex-1 overflow-x-auto rounded-xl border">
              <Table>
                <TableHeader><TableRow><TableHead>시리즈 ID</TableHead><TableHead>지표명 (Title)</TableHead><TableHead>주기</TableHead><TableHead className="text-center">구독 상태</TableHead></TableRow></TableHeader>
                <TableBody>
                  {catalog.items.map((row) => {
                    const active = isSubscribed(row.seriesId);
                    return (
                      <TableRow key={row.seriesId}>
                        <TableCell className="font-bold">{row.seriesId}</TableCell>
                        <TableCell className="text-muted-foreground">{row.title}</TableCell>
                        <TableCell className="text-muted-foreground">{row.frequency || "-"}</TableCell>
                        <TableCell className="text-center">
                          <Button size="sm" variant={active ? "destructive" : "default"}
                            onClick={() => subscriptionMutation.mutate({ method: active ? "DELETE" : "POST", selectedSeriesId: row.seriesId })}
                          >
                            {active ? "해지" : "구독"}
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
            {catalog.items.length < catalog.totalCatalogCount && (
              <Button variant="ghost" className="mx-6 my-2 w-fit"
                onClick={() => setCatalogLimit(prev => prev + 20)}
              >
                <RefreshCw size={14} /> 더보기 ({catalog.items.length} / {catalog.totalCatalogCount})
              </Button>
            )}
          </SectionCard>

          <SectionCard title={`활성 구독 시리즈 (${subscriptions.length})`} icon={Activity}>
            <div className="flex flex-wrap gap-2.5 p-6 flex-1">
              {subscriptions.length === 0 && <p className="w-full px-6 py-10 text-center text-muted-foreground">구독 중인 지표가 없습니다.</p>}
              {subscriptions.map((row) => (
                <Chip key={`fred-${row.seriesId}`} className="w-full justify-between">
                  <div className="flex flex-col gap-0.5">
                    <div className="flex items-center gap-2">
                      <span className="size-2 rounded-full bg-market-positive" />
                      <strong>{row.seriesId}</strong>
                    </div>
                    <span className="text-xs font-medium text-muted-foreground">{row.title}</span>
                  </div>
                  <Button type="button" variant="ghost" size="icon-xs" className="text-destructive"
                    onClick={() => subscriptionMutation.mutate({ method: "DELETE", selectedSeriesId: row.seriesId })}
                    aria-label={`${row.seriesId} 구독 해지`}
                  >
                    <Trash2 size={14} />
                  </Button>
                </Chip>
              ))}
            </div>
          </SectionCard>
        </div>
      )}

      {activeTab === "sync" && (
        <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
          <SectionCard title="FRED 외부 API 탐색" icon={Search}>
            <div className="px-6 py-4 flex flex-col gap-3">
              <p className="text-sm text-muted-foreground">
                FRED 서버에서 직접 지표 정보를 검색하고 메타데이터를 수집합니다.
              </p>
              <div className="flex gap-3 flex-wrap">
                <Input className="w-65" aria-label="FRED 외부 검색" value={externalQuery} onChange={(e) => setExternalQuery(e.target.value)} placeholder="검색 키워드 (시리즈 검색)" />
                <Button variant="outline" onClick={() => void refetchExternalSearch()}>
                  외부 시리즈 검색
                </Button>
                <Badge variant="secondary" className="self-center">검색 결과: {searchSeriesCount}건</Badge>
              </div>
              <div className="flex gap-3 flex-wrap">
                <Input className="w-65" aria-label="FRED 시리즈 ID" value={seriesId} onChange={(e) => setSeriesId(e.target.value.toUpperCase())} placeholder="시리즈 ID (상세정보/관측치)" />
                <Button onClick={() => void refetchInfo()}>
                  상세정보 조회
                </Button>
                <Button onClick={() => void refetchObservations()}>
                  관측치 조회
                </Button>
              </div>
              <div className="flex gap-2.5 flex-wrap">
                <Badge variant="secondary">상세정보 로드: {infoResult ? "성공" : "없음"}</Badge>
                <Badge variant="secondary">관측치 수: {observationCount}건</Badge>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="카탈로그 동기화" icon={RefreshCw}>
            <div className="px-6 py-4 flex flex-col gap-3">
              <p className="text-sm text-muted-foreground">
                로컬 DB의 카탈로그 정보를 최신 데이터와 동기화합니다.
              </p>
              <div className="flex gap-3 flex-wrap">
                <Input className="w-40" aria-label="최대 카테고리 수" value={maxCategories} onChange={(e) => setMaxCategories(e.target.value)} placeholder="최대 카테고리 수" />
                <Input className="w-40" aria-label="페이지 크기" value={pageSize} onChange={(e) => setPageSize(e.target.value)} placeholder="페이지 크기" />
                <Button variant="outline" onClick={() => syncMutation.mutate()}>
                  카탈로그 동기화 시작
                </Button>
              </div>
            </div>
          </SectionCard>
        </div>
      )}

      <StatusBar message={getStatusMessage()} loading={loading} />
    </div>
  );
}
