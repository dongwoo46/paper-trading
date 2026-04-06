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
    <div style={{ display: "flex", flexDirection: "column", gap: "10px", marginTop: "20px" }}>
      <div className="sub-tabs">
        <button className={`sub-tab-btn ${activeTab === "catalog" ? "active" : ""}`} onClick={() => setActiveTab("catalog")}>
          경제지표 탐색
        </button>
        <button className={`sub-tab-btn ${activeTab === "sync" ? "active" : ""}`} onClick={() => setActiveTab("sync")}>
          데이터 연동 및 품질 관리
        </button>
      </div>

      {activeTab === "catalog" && (
        <div className="feature-grid">
          <SectionCard
            title="FRED 경제지표 카탈로그"
            icon={Database}
            headerAction={(
              <div className="form-row">
                <input
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    if (!e.target.value.trim()) setIsSearching(false);
                  }}
                  onKeyDown={(e) => e.key === "Enter" && setIsSearching(true)}
                  placeholder="시리즈 ID 또는 지표명"
                  style={{ width: "220px" }}
                />
                <input value={category} onChange={(e) => setCategory(e.target.value)} placeholder="카테고리" style={{ width: "120px" }} />
                <select value={frequency} onChange={(e) => setFrequency(e.target.value)} style={{ width: "130px" }}>
                  <option value="">전체 주기</option>
                  <option value="D">일간 (Daily)</option>
                  <option value="W">주간 (Weekly)</option>
                  <option value="M">월간 (Monthly)</option>
                  <option value="Q">분기 (Quarterly)</option>
                  <option value="A">연간 (Annual)</option>
                </select>
                <select value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)} style={{ width: "130px" }}>
                  <option value="">전체 상태</option>
                  <option value="SUBSCRIBED">구독 중</option>
                  <option value="UNSUBSCRIBED">미구독</option>
                </select>
                <button className="btn btn-outline" onClick={() => setIsSearching(true)}>
                  <Search size={14} />
                </button>
              </div>
            )}
          >
            <div className="meta-row">
              <span>전체 카탈로그: {catalog.totalCatalogCount}</span>
              <span>전체 구독 중: {catalog.totalSubscribedCount}</span>
              <span>조회 결과: {catalog.returnedCount}</span>
            </div>
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>시리즈 ID</th>
                    <th>지표명 (Title)</th>
                    <th>주기</th>
                    <th style={{ textAlign: "center" }}>구독 상태</th>
                  </tr>
                </thead>
                <tbody>
                  {catalog.items.map((row) => {
                    const active = isSubscribed(row.seriesId);
                    return (
                      <tr key={row.seriesId}>
                        <td style={{ fontWeight: 700, color: "var(--brand-primary)" }}>{row.seriesId}</td>
                        <td style={{ fontSize: "13px" }}>{row.title}</td>
                        <td>{row.frequency || "-"}</td>
                        <td style={{ textAlign: "center" }}>
                          <button
                            className={`btn ${active ? "btn-danger" : "btn-primary"}`}
                            style={{ padding: "6px 12px", fontSize: "12px" }}
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
              <button className="load-more-btn" onClick={() => setCatalogLimit(prev => prev + 20)}>
                <RefreshCw size={14} /> 더보기 ({catalog.items.length} / {catalog.totalCatalogCount})
              </button>
            )}
          </SectionCard>

          <SectionCard title={`활성 구독 시리즈 (${subscriptions.length})`} icon={Activity}>
            <div className="chips-container" style={{ flex: 1 }}>
              {subscriptions.length === 0 && <p className="empty-state">구독 중인 지표가 없습니다.</p>}
              {subscriptions.map((row) => (
                <Chip key={`fred-${row.seriesId}`} style={{ width: "100%", justifyContent: "space-between" }}>
                  <div style={{ display: "flex", flexDirection: "column", gap: "2px" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                      <span className="chip-status"></span>
                      <strong>{row.seriesId}</strong>
                    </div>
                    <span style={{ color: "var(--text-secondary)", fontSize: "11px", fontWeight: 500 }}>{row.title}</span>
                  </div>
                  <button
                    onClick={() => subscriptionMutation.mutate({ method: "DELETE", selectedSeriesId: row.seriesId })}
                    style={{ color: "var(--status-error)", border: "none", background: "transparent", cursor: "pointer", display: "flex" }}
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
        <div className="feature-grid">
          <SectionCard title="FRED 외부 API 탐색" icon={Search}>
            <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <p style={{ color: "var(--text-secondary)", fontSize: "14px" }}>
                FRED 서버에서 직접 지표 정보를 검색하고 메타데이터를 수집합니다.
              </p>
              <div className="form-row">
                <input value={externalQuery} onChange={(e) => setExternalQuery(e.target.value)} placeholder="검색 키워드 (시리즈 검색)" style={{ width: "260px" }} />
                <button className="btn btn-outline" onClick={() => void refetchExternalSearch()}>
                  외부 시리즈 검색
                </button>
                <span style={{ color: "var(--text-secondary)", fontSize: "12px", alignSelf: "center" }}>검색 결과: {searchSeriesCount}건</span>
              </div>
              <div className="form-row">
                <input value={seriesId} onChange={(e) => setSeriesId(e.target.value.toUpperCase())} placeholder="시리즈 ID (상세정보/관측치)" style={{ width: "260px" }} />
                <button className="btn btn-primary" onClick={() => void refetchInfo()}>
                  상세정보 조회
                </button>
                <button className="btn btn-primary" onClick={() => void refetchObservations()}>
                  관측치 조회
                </button>
              </div>
              <div className="meta-row" style={{ padding: 0 }}>
                <span>상세정보 로드: {infoResult ? "성공" : "없음"}</span>
                <span>관측치 수: {observationCount}건</span>
              </div>
            </div>
          </SectionCard>

          <SectionCard title="카탈로그 동기화" icon={RefreshCw}>
            <div style={{ padding: "16px 24px", display: "flex", flexDirection: "column", gap: "12px" }}>
              <p style={{ color: "var(--text-secondary)", fontSize: "14px" }}>
                로컬 DB의 카탈로그 정보를 최신 데이터와 동기화합니다.
              </p>
              <div className="form-row">
                <input value={maxCategories} onChange={(e) => setMaxCategories(e.target.value)} placeholder="최대 카테고리 수" style={{ width: "160px" }} />
                <input value={pageSize} onChange={(e) => setPageSize(e.target.value)} placeholder="페이지 크기" style={{ width: "160px" }} />
                <button className="btn btn-outline" onClick={() => syncMutation.mutate()}>
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
