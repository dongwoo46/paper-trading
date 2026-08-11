import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAccounts } from "../../../entities/account/api/accountApi";
import {
  fetchTaxSummary,
  recalculateTaxSummary,
} from "../../../entities/tax-summary/api/taxSummaryApi";
import { TaxSummaryActionPanel } from "../../../features/tax-summary/ui/TaxSummaryActionPanel";
import { TaxSummarySelectionPanel } from "../../../features/tax-summary/ui/TaxSummarySelectionPanel";
import { TaxSummarySummaryPanel } from "../../../features/tax-summary/ui/TaxSummarySummaryPanel";
import { Alert, AlertDescription } from "@/shared/ui/shadcn/alert";
import { Skeleton } from "@/shared/ui/shadcn/skeleton";

function parseStatusCode(error: unknown): number | null {
  if (!(error instanceof Error)) return null;
  const match = error.message.match(/^(\d{3})\s/);
  return match ? Number(match[1]) : null;
}

function getSummaryErrorMessage(error: unknown): string {
  const status = parseStatusCode(error);
  if (status === 400) return "요청 값이 올바르지 않습니다. 계좌/연도를 다시 확인하세요.";
  if (status === 404) return "선택한 계좌/연도의 세금 요약이 없습니다. 연도를 변경하거나 재계산을 실행하세요.";
  return "세금 요약을 불러오지 못했습니다.";
}

function getRecalculateErrorMessage(error: unknown): string {
  const status = parseStatusCode(error);
  if (status === 409) return "이미 재계산이 진행 중입니다. 완료 후 다시 확인하세요.";
  if (status === 400) return "재계산 요청 값이 올바르지 않습니다. 계좌/연도를 다시 확인하세요.";
  if (status === 404) return "선택한 계좌를 찾을 수 없습니다. 계좌를 다시 선택하세요.";
  return "세금 재계산 요청에 실패했습니다.";
}

export function TaxSummaryPage() {
  const queryClient = useQueryClient();
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [taxYear, setTaxYear] = useState(new Date().getFullYear());

  const { data: accounts = [], isLoading: isAccountsLoading, isError: isAccountsError } = useQuery({
    queryKey: ["accounts", "list"],
    queryFn: fetchAccounts,
    staleTime: 30_000,
  });

  const resolvedAccountId = useMemo(
    () => selectedAccountId ?? (accounts.length > 0 ? accounts[0].id : null),
    [accounts, selectedAccountId]
  );

  const summaryQuery = useQuery({
    queryKey: ["tax-summary", resolvedAccountId, taxYear],
    queryFn: () => fetchTaxSummary(resolvedAccountId!, taxYear),
    enabled: resolvedAccountId !== null,
    retry: false,
    staleTime: 0,
  });

  const recalculateMutation = useMutation({
    mutationFn: () => recalculateTaxSummary(resolvedAccountId!, taxYear),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["tax-summary", resolvedAccountId, taxYear] });
    },
  });

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <h2 className="text-[28px] font-bold tracking-tight">세금 요약</h2>
        <p className="max-w-3xl text-sm text-muted-foreground sm:text-base">계좌와 연도를 선택해 세금 요약을 조회하고 수동 재계산을 실행할 수 있습니다.</p>
      </div>

      {isAccountsLoading && (
        <Skeleton className="h-24 w-full" aria-label="계좌 정보 로딩 중">
          <span className="sr-only">계좌 정보를 불러오는 중...</span>
        </Skeleton>
      )}
      {isAccountsError && <Alert variant="destructive"><AlertDescription>계좌 목록을 불러오지 못했습니다.</AlertDescription></Alert>}
      {!isAccountsLoading && !isAccountsError && accounts.length === 0 && (
        <Alert><AlertDescription>등록된 계좌가 없습니다.</AlertDescription></Alert>
      )}

      {resolvedAccountId !== null && (
        <>
          <TaxSummarySelectionPanel
            accounts={accounts}
            accountId={resolvedAccountId}
            taxYear={taxYear}
            onChangeAccountId={setSelectedAccountId}
            onChangeTaxYear={setTaxYear}
          />
          <TaxSummaryActionPanel
            isDisabled={
              recalculateMutation.isPending ||
              summaryQuery.data?.status === "RUNNING" ||
              parseStatusCode(recalculateMutation.error) === 409
            }
            onRecalculate={() => {
              if (!recalculateMutation.isPending && parseStatusCode(recalculateMutation.error) !== 409) {
                recalculateMutation.mutate();
              }
            }}
          />
          {recalculateMutation.isError && (
            <Alert variant="destructive"><AlertDescription>{getRecalculateErrorMessage(recalculateMutation.error)}</AlertDescription></Alert>
          )}

          {summaryQuery.isLoading && <Skeleton className="h-48 w-full" aria-label="세금 요약 로딩 중" />}
          {summaryQuery.isError && (
            <Alert variant="destructive"><AlertDescription>{getSummaryErrorMessage(summaryQuery.error)}</AlertDescription></Alert>
          )}
          {!summaryQuery.isLoading && !summaryQuery.isError && summaryQuery.data === null && (
            <Alert><AlertDescription>세금 요약 데이터가 없습니다.</AlertDescription></Alert>
          )}
          {summaryQuery.data && <TaxSummarySummaryPanel summary={summaryQuery.data} />}
        </>
      )}
    </section>
  );
}
