import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, Database, RefreshCw, Search, Trash2 } from "lucide-react";
import type { CatalogResponse, FredCatalogItem } from "../../../entities/symbol/model/types";
import { fetchJson } from "../../../shared/api";
import { Chip, SectionCard, StatusBar } from "../../../shared/ui";

type StatusFilter = "" | "SUBSCRIBED" | "UNSUBSCRIBED";

const EMPTY: CatalogResponse<FredCatalogItem> = {
  items: [],
  returnedCount: 0,
  totalCatalogCount: 0,
  totalSubscribedCount: 0
};

const INPUT_CLS = "bg-bg-input border border-white/12 text-text-primary px-4 py-3 rounded-xl outline-none transition-all w-full focus:border-brand-primary focus:shadow-[0_0_0_4px_rgba(96,165,250,0.25)] focus:bg-bg-card";
const SELECT_CLS = "bg-bg-input border border-white/12 text-text-primary px-4 py-3 rounded-xl outline-none transition-all focus:border-brand-primary focus:shadow-[0_0_0_4px_rgba(96,165,250,0.25)] focus:bg-bg-card";
const BTN_BASE = "inline-flex items-center justify-center gap-2.5 px-5 py-3 rounded-xl font-semibold text-sm cursor-pointer transition-all border whitespace-nowrap";
const BTN_PRIMARY = `${BTN_BASE} bg-gradient-to-br from-blue-500 to-emerald-500 text-white shadow border-transparent hover:-translate-y-0.5 hover:brightness-110`;
const BTN_OUTLINE = `${BTN_BASE} bg-transparent border-white/12 text-text-primary hover:bg-white/5 hover:border-text-muted`;
const BTN_PRIMARY_SM = "inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl font-semibold text-xs cursor-pointer transition-all border border-transparent bg-gradient-to-br from-blue-500 to-emerald-500 text-white shadow hover:brightness-110 whitespace-nowrap";
const BTN_DANGER_SM = "inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl font-semibold text-xs cursor-pointer transition-all border bg-red-500/8 text-red-500 border-red-500/20 hover:bg-red-500/15 whitespace-nowrap";

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

  const subTabCls = (active: boolean) =>
    `px-4 py-2 rounded-xl text-xs font-semibold transition-all ${active ? "bg-blue-400/25 text-brand-primary" : "text-text-muted hover:text-text-primary"}`;

  return (
    <div className="flex flex-col gap-2.5 mt-5">
      <div className="flex gap-2 p-1 bg-black/20 rounded-[16px] mb-5 border border-white/12 w-fit">
        <button className={subTabCls(activeTab === "catalog")} onClick={() => setActiveTab("catalog")}>
          경제지표 탐색
        </button>
        <button className={subTabCls(activeTab === "sync")} onClick={() => setActiveTab("sync")}>
          데이터 연동 및 품질 관리
        </button>
      </div>

      {activeTab === "catalog" && (
        <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
          <SectionCard
            title="FRED 경제지표 카탈로그"
            icon={Database}
            headerAction={(
              <div className="flex gap-3 flex-wrap">
                <input
                  className={`${INPUT_CLS} w-[220px]`}
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    if (!e.target.value.trim()) setIsSearching(false);
                  }}
                  onKeyDown={(e) => e.key === "Enter" && setIsSearching(true)}
                  placeholder="시리즈 ID 또는 지표명"
                />
                <input className={`${INPUT_CLS} w-[120px]`} value={category} onChange={(e) => setCategory(e.target.value)} placeholder="카테고리" />
                <select className={`${SELECT_CLS} w-[130px]`} value={frequency} onChange={(e) => setFrequency(e.target.value)}>
                  <option value="">전체 주기</option>
                  <option value="D">일간 (Daily)</option>
                  <option value="W">주간 (Weekly)</option>
                  <option value="M">월간 (Monthly)</option>
                  <option value="Q">분기 (Quarterly)</option>
                  <option value="A">연간 (Annual)</option>
                </select>
                <select className={`${SELECT_CLS} w-[130px]`} value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}>
                  <option value="">전체 상태</option>
                  <option value="SUBSCRIBED">구독 중</option>
                  <option value="UNSUBSCRIBED">미구독</option>
                </select>
                <button className={BTN_OUTLINE} onClick={() => setIsSearching(true)}>
                  <Search size={14} />
                </button>
              </div>
            )}
          >
            <div className="flex gap-2.5 flex-wrap px-6 pb-3.5">
              <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1.5 bg-white/[0.02]">전체 카탈로그: {catalog.totalCatalogCount}</span>
              <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1.5 bg-white/[0.02]">전체 구독 중: {catalog.totalSubscribedCount}</span>
              <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1.5 bg-white/[0.02]">조회 결과: {catalog.returnedCount}</span>
            </div>
            <div className="overflow-x-auto rounded-[16px] bg-black/20 border border-white/12 flex-1 min-h-[300px]">
              <table className="w-full border-collapse">
                <thead>
                  <tr>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">시리즈 ID</th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">지표명 (Title)</th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">주기</th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-center text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">구독 상태</th>
                  </tr>
                </thead>
                <tbody>
                  {catalog.items.map((row) => {
                    const active = isSubscribed(row.seriesId);
                    return (
                      <tr key={row.seriesId} className="hover:bg-white/[0.02]">
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap font-bold text-brand-primary">{row.seriesId}</td>
                        <td className="px-6 py-4 text-[13px] border-b border-white/12 whitespace-nowrap text-text-secondary">{row.title}</td>
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">{row.frequency || "-"}</td>
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-center">
                          <button
                            className={active ? BTN_DANGER_SM : BTN_PRIMARY_SM}
                            onClick={() => subscriptionMutation.mutate({ method: active ? "DELETE" : "POST", selectedSeriesId: row.seriesId })}
                          >
                            {active ? "해지" : "구독"}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {catalog.items.length < catalog.totalCatalogCount && (
              <button
                className="inline-flex items-center gap-1.5 px-4 py-2 my-2 mx-6 text-[13px] font-semibold text-text-muted bg-transparent border border-white/12 rounded-lg cursor-pointer transition-all hover:bg-white/5 hover:text-text-primary"
                onClick={() => setCatalogLimit(prev => prev + 20)}
              >
                <RefreshCw size={14} /> 더보기 ({catalog.items.length} / {catalog.totalCatalogCount})
              </button>
            )}
          </SectionCard>

          <SectionCard title={`활성 구독 시리즈 (${subscriptions.length})`} icon={Activity}>
            <div className="flex flex-wrap gap-2.5 p-6 flex-1">
              {subscriptions.length === 0 && <p className="py-10 px-6 text-center text-text-muted italic w-full">구독 중인 지표가 없습니다.</p>}
              {subscriptions.map((row) => (
                <Chip key={`fred-${row.seriesId}`} className="w-full justify-between">
                  <div className="flex flex-col gap-0.5">
                    <div className="flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full bg-status-success shadow-[0_0_8px_var(--status-success)]" />
                      <strong>{row.seriesId}</strong>
                    </div>
                    <span className="text-text-secondary text-[11px] font-medium">{row.title}</span>
                  </div>
                  <button
                    onClick={() => subscriptionMutation.mutate({ method: "DELETE", selectedSeriesId: row.seriesId })}
                    className="text-status-error border-none bg-transparent cursor-pointer flex"
                  >
                    <Trash2 size={14} />
                  </button>
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
              <p className="text-text-secondary text-sm">
                FRED 서버에서 직접 지표 정보를 검색하고 메타데이터를 수집합니다.
              </p>
              <div className="flex gap-3 flex-wrap">
                <input className={`${INPUT_CLS} w-[260px]`} value={externalQuery} onChange={(e) => setExternalQuery(e.target.value)} placeholder="검색 키워드 (시리즈 검색)" />
                <button className={BTN_OUTLINE} onClick={() => void refetchExternalSearch()}>
                  외부 시리즈 검색
                </button>
                <span className="text-text-secondary text-xs self-center">검색 결과: {searchSeriesCount}건</span>
              </div>
              <div className="flex gap-3 flex-wrap">
                <input className={`${INPUT_CLS} w-[260px]`} value={seriesId} onChange={(e) => setSeriesId(e.target.value.toUpperCase())} placeholder="시리즈 ID (상세정보/관측치)" />
                <button className={BTN_PRIMARY} onClick={() => void refetchInfo()}>
                  상세정보 조회
                </button>
                <button className={BTN_PRIMARY} onClick={() => void refetchObservations()}>
                  관측치 조회
                </button>
              </div>
              <div className="flex gap-2.5 flex-wrap">
                <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1.5 bg-white/[0.02]">상세정보 로드: {infoResult ? "성공" : "없음"}</span>
                <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1.5 bg-white/[0.02]">관측치 수: {observationCount}건</span>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="카탈로그 동기화" icon={RefreshCw}>
            <div className="px-6 py-4 flex flex-col gap-3">
              <p className="text-text-secondary text-sm">
                로컬 DB의 카탈로그 정보를 최신 데이터와 동기화합니다.
              </p>
              <div className="flex gap-3 flex-wrap">
                <input className={`${INPUT_CLS} w-[160px]`} value={maxCategories} onChange={(e) => setMaxCategories(e.target.value)} placeholder="최대 카테고리 수" />
                <input className={`${INPUT_CLS} w-[160px]`} value={pageSize} onChange={(e) => setPageSize(e.target.value)} placeholder="페이지 크기" />
                <button className={BTN_OUTLINE} onClick={() => syncMutation.mutate()}>
                  카탈로그 동기화 시작
                </button>
              </div>
            </div>
          </SectionCard>
        </div>
      )}

      <StatusBar message={getStatusMessage()} loading={loading} />
    </div>
  );
}
