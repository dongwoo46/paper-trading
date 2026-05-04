import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAccounts } from "../../../entities/account/api/accountApi";
import {
  fetchTradingJournalDetail,
  fetchTradingJournalList,
  updateTradingJournal,
} from "../../../entities/trading-journal/api/tradingJournalApi";
import type { JournalSentiment } from "../../../entities/trading-journal/model/types";
import { TradingJournalDetailPanel } from "../../../features/trading-journal/ui/TradingJournalDetailPanel";
import { TradingJournalFilterPanel } from "../../../features/trading-journal/ui/TradingJournalFilterPanel";
import { TradingJournalListPanel } from "../../../features/trading-journal/ui/TradingJournalListPanel";

function yyyyMmDd(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function TradingJournalPage() {
  const queryClient = useQueryClient();
  const [tickerInput, setTickerInput] = useState("");
  const [ticker, setTicker] = useState("");
  const [from, setFrom] = useState(yyyyMmDd(new Date(Date.now() - 1000 * 60 * 60 * 24 * 30)));
  const [to, setTo] = useState(yyyyMmDd(new Date()));
  const [selectedJournalId, setSelectedJournalId] = useState<number | null>(null);

  const { data: accounts = [] } = useQuery({
    queryKey: ["accounts", "list"],
    queryFn: fetchAccounts,
    staleTime: 30_000,
  });

  const accountId = useMemo(() => (accounts.length > 0 ? accounts[0].id : null), [accounts]);

  const listQuery = useQuery({
    queryKey: ["trading-journals", accountId, ticker, from, to, 0, 20],
    queryFn: () => fetchTradingJournalList({ accountId: accountId!, ticker: ticker || undefined, from, to, page: 0, size: 20 }),
    enabled: accountId !== null,
  });

  const detailQuery = useQuery({
    queryKey: ["trading-journal-detail", selectedJournalId],
    queryFn: () => fetchTradingJournalDetail(selectedJournalId!),
    enabled: selectedJournalId !== null,
  });

  const updateMutation = useMutation({
    mutationFn: (body: { title: string; content: string; sentiment: JournalSentiment }) =>
      updateTradingJournal(selectedJournalId!, body),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["trading-journals"] }),
        queryClient.invalidateQueries({ queryKey: ["trading-journal-detail", selectedJournalId] }),
      ]);
    },
  });

  return (
    <section>
      <div className="panel-header">
        <h2>거래 일지</h2>
      </div>
      <TradingJournalFilterPanel
        ticker={tickerInput}
        from={from}
        to={to}
        onChangeTicker={setTickerInput}
        onChangeFrom={setFrom}
        onChangeTo={setTo}
        onSubmit={() => setTicker(tickerInput.trim())}
      />
      <div style={{ display: "grid", gap: 16, gridTemplateColumns: "1fr 1fr" }}>
        <TradingJournalListPanel
          items={listQuery.data?.items ?? []}
          selectedJournalId={selectedJournalId}
          isLoading={listQuery.isLoading}
          isError={listQuery.isError}
          onSelect={setSelectedJournalId}
        />
        <TradingJournalDetailPanel
          detail={detailQuery.data ?? null}
          isLoading={detailQuery.isLoading}
          isError={detailQuery.isError}
          isSaving={updateMutation.isPending}
          onSave={(body) => {
            if (!updateMutation.isPending && selectedJournalId !== null) updateMutation.mutate(body);
          }}
        />
      </div>
    </section>
  );
}
