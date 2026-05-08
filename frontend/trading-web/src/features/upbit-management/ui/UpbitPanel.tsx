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

const INPUT_CLS = "bg-bg-input border border-white/12 text-text-primary px-4 py-3 rounded-xl outline-none transition-all w-full focus:border-brand-primary focus:shadow-[0_0_0_4px_rgba(96,165,250,0.25)] focus:bg-bg-card";
const SELECT_CLS = "bg-bg-input border border-white/12 text-text-primary px-4 py-3 rounded-xl outline-none transition-all focus:border-brand-primary focus:shadow-[0_0_0_4px_rgba(96,165,250,0.25)] focus:bg-bg-card";
const BTN_BASE = "inline-flex items-center justify-center gap-2.5 px-5 py-3 rounded-xl font-semibold text-sm cursor-pointer transition-all border whitespace-nowrap";
const BTN_PRIMARY_SM = "inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl font-semibold text-xs cursor-pointer transition-all border border-transparent bg-gradient-to-br from-blue-500 to-emerald-500 text-white shadow hover:brightness-110 whitespace-nowrap";
const BTN_DANGER_SM = "inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl font-semibold text-xs cursor-pointer transition-all border bg-red-500/8 text-red-500 border-red-500/20 hover:bg-red-500/15 whitespace-nowrap";
const BTN_OUTLINE = `${BTN_BASE} bg-transparent border-white/12 text-text-primary hover:bg-white/5 hover:border-text-muted`;

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
    <div className="flex flex-col gap-6 mt-5">
      <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
        <SectionCard
          title="업비트 종목 카탈로그"
          icon={Cpu}
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
                placeholder="마켓 또는 종목명 검색"
              />
              <select className={`${SELECT_CLS} w-[130px]`} value={marketGroup} onChange={(e) => setMarketGroup(e.target.value)}>
                <option value="">전체 그룹</option>
                <option value="KRW">KRW</option>
                <option value="BTC">BTC</option>
                <option value="USDT">USDT</option>
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
          <div className="overflow-x-auto rounded-[16px] bg-black/20 border border-white/12 flex-1 min-h-[300px] max-h-[520px]">
            <table className="w-full border-collapse">
              <thead>
                <tr>
                  <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">마켓</th>
                  <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">종목명 (KOR)</th>
                  <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">그룹</th>
                  <th className="bg-white/[0.02] px-6 py-3.5 text-center text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">구독 상태</th>
                </tr>
              </thead>
              <tbody>
                {catalog.items.map((row) => {
                  const active = isSubscribed(row.market);
                  const koreanName = row.koreanName ?? row.name ?? "-";
                  return (
                    <tr key={row.market} className="hover:bg-white/[0.02]">
                      <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap font-bold text-brand-primary">{row.market}</td>
                      <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">{koreanName}</td>
                      <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">{row.marketGroup}</td>
                      <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-center">
                        <button
                          className={active ? BTN_DANGER_SM : BTN_PRIMARY_SM}
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
            <button
              className="inline-flex items-center gap-1.5 px-4 py-2 my-2 mx-6 text-[13px] font-semibold text-text-muted bg-transparent border border-white/12 rounded-lg cursor-pointer transition-all hover:bg-white/5 hover:text-text-primary"
              onClick={() => setCatalogLimit(prev => prev + 20)}
            >
              <RefreshCw size={14} /> 더보기 ({catalog.items.length} / {catalog.totalCatalogCount})
            </button>
          )}
        </SectionCard>

        <SectionCard title={`활성 구독 마켓 (${subscriptions.length})`} icon={Activity}>
          <div className="flex flex-wrap gap-2.5 p-6">
            {subscriptions.length === 0 && <p className="py-10 px-6 text-center text-text-muted italic w-full">구독 중인 마켓이 없습니다.</p>}
            {subscriptions.map((row) => (
              <Chip key={row.market} className="w-full justify-between">
                <div className="flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-status-success shadow-[0_0_8px_var(--status-success)]" />
                  <strong>{row.market}</strong>
                  <span className="text-text-secondary text-xs">{row.koreanName ?? row.englishName ?? row.name ?? "-"}</span>
                </div>
                <button
                  onClick={() => subscriptionMutation.mutate({ method: "DELETE", market: row.market })}
                  className="text-status-error border-none bg-transparent cursor-pointer flex"
                >
                  <Trash2 size={14} />
                </button>
              </Chip>
            ))}
          </div>
        </SectionCard>
      </div>

      <SectionCard title="카탈로그 동기화" icon={RefreshCw}>
        <div className="px-6 py-4">
          <button className={BTN_OUTLINE} onClick={() => syncMutation.mutate()}>
            업비트 시장 정보 동기화 실행
          </button>
        </div>
      </SectionCard>

      <StatusBar message={getStatusMessage()} loading={loading} />
    </div>
  );
}
