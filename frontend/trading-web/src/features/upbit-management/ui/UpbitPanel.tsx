import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, Cpu, RefreshCw, Search, Trash2 } from "lucide-react";
import type { CatalogResponse, UpbitCatalogItem } from "../../../entities/symbol/model/types";
import { fetchJson } from "../../../shared/api";
import { Chip, SectionCard, StatusBar } from "../../../shared/ui";

type StatusFilter = "" | "SUBSCRIBED" | "UNSUBSCRIBED";

const EMPTY: CatalogResponse<UpbitCatalogItem> = {
  items: [],
  returnedCount: 0,
  totalCatalogCount: 0,
  totalSubscribedCount: 0
};

export function UpbitPanel() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [catalogLimit, setCatalogLimit] = useState(20);
  const [marketGroup, setMarketGroup] = useState("");
  const [status, setStatus] = useState<StatusFilter>("");
  const [isSearching, setIsSearching] = useState(false);

  const { data: catalog = EMPTY, isLoading: isCatalogLoading } = useQuery({
    queryKey: ["upbit", "catalog", query, marketGroup, status, isSearching, catalogLimit],
    queryFn: () => {
      const params = new URLSearchParams({ limit: catalogLimit.toString() });
      if (isSearching && query.trim()) params.set("query", query.trim());
      if (marketGroup) params.set("marketGroup", marketGroup);
      if (status) params.set("status", status);
      const path = isSearching ? "/api/upbit/markets/search" : "/api/upbit/markets/catalog";
      return fetchJson<CatalogResponse<UpbitCatalogItem>>(`${path}?${params.toString()}`);
    }
  });

  const { data: subscriptions = [], isLoading: isSubscriptionsLoading } = useQuery({
    queryKey: ["upbit", "subscriptions"],
    queryFn: () => fetchJson<UpbitCatalogItem[]>("/api/upbit/markets/subscriptions")
  });

  const subscriptionMutation = useMutation({
    mutationFn: ({ method, market }: { method: "POST" | "DELETE"; market: string }) =>
      fetchJson("/api/upbit/markets/subscriptions", {
        method,
        body: JSON.stringify({ market })
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["upbit"] });
    }
  });

  const syncMutation = useMutation({
    mutationFn: () => fetchJson<{ status: string; processed: number }>("/api/upbit/markets/catalog/sync", { method: "POST" }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["upbit", "catalog"] });
    }
  });

  const isSubscribed = (market: string) => subscriptions.some((item) => item.market === market);
  const loading = isCatalogLoading || isSubscriptionsLoading || subscriptionMutation.isPending || syncMutation.isPending;
  
  const getStatusMessage = () => {
    if (subscriptionMutation.isError || syncMutation.isError) return "요청 실패";
    if (subscriptionMutation.isSuccess) return "구독 정보가 업데이트되었습니다.";
    if (syncMutation.isSuccess) return "카탈로그 동기화가 완료되었습니다.";
    return "준비 완료";
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px", marginTop: "20px" }}>
      <div className="feature-grid">
        <SectionCard
          title="업비트 종목 카탈로그"
          icon={Cpu}
          headerAction={(
            <div className="form-row">
              <input
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  if (!e.target.value.trim()) setIsSearching(false);
                }}
                onKeyDown={(e) => e.key === "Enter" && setIsSearching(true)}
                placeholder="마켓 또는 종목명 검색"
                style={{ width: "220px" }}
              />
              <select value={marketGroup} onChange={(e) => setMarketGroup(e.target.value)} style={{ width: "130px" }}>
                <option value="">전체 그룹</option>
                <option value="KRW">KRW</option>
                <option value="BTC">BTC</option>
                <option value="USDT">USDT</option>
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
          <div className="table-container" style={{ maxHeight: "520px" }}>
            <table>
              <thead>
                <tr>
                  <th>마켓</th>
                  <th>종목명 (KOR)</th>
                  <th>그룹</th>
                  <th style={{ textAlign: "center" }}>구독 상태</th>
                </tr>
              </thead>
              <tbody>
                {catalog.items.map((row) => {
                  const active = isSubscribed(row.market);
                  const koreanName = row.koreanName ?? row.name ?? "-";
                  return (
                    <tr key={row.market}>
                      <td style={{ fontWeight: 700, color: "var(--brand-primary)" }}>{row.market}</td>
                      <td>{koreanName}</td>
                      <td>{row.marketGroup}</td>
                      <td style={{ textAlign: "center" }}>
                        <button
                          className={`btn ${active ? "btn-danger" : "btn-primary"}`}
                          style={{ padding: "6px 12px", fontSize: "12px" }}
                          onClick={() => subscriptionMutation.mutate({ method: active ? "DELETE" : "POST", market: row.market })}
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

        <SectionCard title={`활성 구독 마켓 (${subscriptions.length})`} icon={Activity}>
          <div className="chips-container">
            {subscriptions.length === 0 && <p className="empty-state">구독 중인 마켓이 없습니다.</p>}
            {subscriptions.map((row) => (
              <Chip key={row.market} style={{ width: "100%", justifyContent: "space-between" }}>
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <span className="chip-status"></span>
                  <strong>{row.market}</strong>
                  <span style={{ color: "var(--text-secondary)", fontSize: "12px" }}>{row.koreanName ?? row.englishName ?? row.name ?? "-"}</span>
                </div>
                <button
                  onClick={() => subscriptionMutation.mutate({ method: "DELETE", market: row.market })}
                  style={{ color: "var(--status-error)", border: "none", background: "transparent", cursor: "pointer", display: "flex" }}
                >
                  <Trash2 size={14} />
                </button>
              </Chip>
            ))}
          </div>
        </SectionCard>
      </div>

      <SectionCard title="카탈로그 동기화" icon={RefreshCw}>
        <div style={{ padding: "16px 24px" }}>
          <button className="btn btn-outline" onClick={() => syncMutation.mutate()}>
            업비트 시장 정보 동기화 실행
          </button>
        </div>
      </SectionCard>

      <StatusBar message={getStatusMessage()} loading={loading} />
    </div>
  );
}
