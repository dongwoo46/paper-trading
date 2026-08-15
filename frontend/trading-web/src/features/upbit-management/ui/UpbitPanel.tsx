import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Activity, Cpu, RefreshCw, Search, Trash2 } from "lucide-react";
import type { CatalogResponse, UpbitCatalogItem } from "../../../entities/symbol/model/types";
import { fetchJson } from "../../../shared/api";
import { Chip, SectionCard, StatusBar } from "../../../shared/ui";
import { Badge } from "@/shared/ui/shadcn/badge";
import { Button } from "@/shared/ui/shadcn/button";
import { Input } from "@/shared/ui/shadcn/input";
import { NativeSelect, NativeSelectOption } from "@/shared/ui/shadcn/native-select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/ui/shadcn/table";

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
    <div className="flex flex-col gap-6 mt-5">
      <div className="grid grid-cols-[2fr_1fr] gap-6 max-lg:grid-cols-1">
        <SectionCard
          title="업비트 종목 카탈로그"
          icon={Cpu}
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
                placeholder="마켓 또는 종목명 검색"
              />
              <NativeSelect className="w-32" aria-label="마켓 그룹" value={marketGroup} onChange={(e) => setMarketGroup(e.target.value)}><NativeSelectOption value="">전체 그룹</NativeSelectOption><NativeSelectOption value="KRW">KRW</NativeSelectOption><NativeSelectOption value="BTC">BTC</NativeSelectOption><NativeSelectOption value="USDT">USDT</NativeSelectOption></NativeSelect>
              <NativeSelect className="w-32" aria-label="구독 상태" value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}><NativeSelectOption value="">전체 상태</NativeSelectOption><NativeSelectOption value="SUBSCRIBED">구독 중</NativeSelectOption><NativeSelectOption value="UNSUBSCRIBED">미구독</NativeSelectOption></NativeSelect>
              <Button variant="outline" size="icon" onClick={() => setIsSearching(true)} aria-label="업비트 마켓 검색">
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
          <div className="max-h-130 min-h-75 flex-1 overflow-x-auto rounded-xl border">
            <Table>
              <TableHeader><TableRow><TableHead>마켓</TableHead><TableHead>종목명 (KOR)</TableHead><TableHead>그룹</TableHead><TableHead className="text-center">구독 상태</TableHead></TableRow></TableHeader>
              <TableBody>
                {catalog.items.map((row) => {
                  const active = isSubscribed(row.market);
                  const koreanName = row.koreanName ?? row.name ?? "-";
                  return (
                    <TableRow key={row.market}>
                      <TableCell className="font-bold text-primary">{row.market}</TableCell>
                      <TableCell className="text-muted-foreground">{koreanName}</TableCell>
                      <TableCell className="text-muted-foreground">{row.marketGroup}</TableCell>
                      <TableCell className="text-center">
                        <Button size="sm" variant={active ? "destructive" : "default"}
                          onClick={() => subscriptionMutation.mutate({ method: active ? "DELETE" : "POST", market: row.market })}
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

        <SectionCard title={`활성 구독 마켓 (${subscriptions.length})`} icon={Activity}>
          <div className="flex flex-wrap gap-2.5 p-6">
            {subscriptions.length === 0 && <p className="w-full px-6 py-10 text-center text-muted-foreground">구독 중인 마켓이 없습니다.</p>}
            {subscriptions.map((row) => (
              <Chip key={row.market} className="w-full justify-between">
                <div className="flex items-center gap-2">
                  <span className="size-2 rounded-full bg-market-positive" />
                  <strong>{row.market}</strong>
                  <span className="text-xs text-muted-foreground">{row.koreanName ?? row.englishName ?? row.name ?? "-"}</span>
                </div>
                <Button type="button" variant="ghost" size="icon-xs" className="text-destructive"
                  onClick={() => subscriptionMutation.mutate({ method: "DELETE", market: row.market })}
                  aria-label={`${row.market} 구독 해지`}
                >
                  <Trash2 size={14} />
                </Button>
              </Chip>
            ))}
          </div>
        </SectionCard>
      </div>

      <SectionCard title="카탈로그 동기화" icon={RefreshCw}>
        <div className="px-6 py-4">
          <Button variant="outline" onClick={() => syncMutation.mutate()}>
            업비트 시장 정보 동기화 실행
          </Button>
        </div>
      </SectionCard>

      <StatusBar message={getStatusMessage()} loading={loading} />
    </div>
  );
}
