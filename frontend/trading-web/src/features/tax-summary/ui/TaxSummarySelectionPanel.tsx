import type { AccountResponse } from "../../../entities/account/model/types";
import { Label } from "@/shared/ui/shadcn/label";
import { NativeSelect, NativeSelectOption } from "@/shared/ui/shadcn/native-select";

interface TaxSummarySelectionPanelProps {
  accounts: AccountResponse[];
  accountId: number;
  taxYear: number;
  onChangeAccountId: (accountId: number) => void;
  onChangeTaxYear: (taxYear: number) => void;
}

export function TaxSummarySelectionPanel({
  accounts,
  accountId,
  taxYear,
  onChangeAccountId,
  onChangeTaxYear,
}: TaxSummarySelectionPanelProps) {
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 6 }, (_, index) => currentYear - index);

  return (
    <div className="mb-4 grid gap-3 sm:grid-cols-2">
      <div className="grid gap-1.5">
        <Label htmlFor="tax-account">계좌 선택</Label>
        <NativeSelect id="tax-account" className="w-full" value={accountId} onChange={(event) => onChangeAccountId(parseInt(event.target.value, 10))}>
          {accounts.map((account) => (
            <NativeSelectOption key={account.id} value={account.id}>
              {account.accountName}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="tax-year">연도 선택</Label>
        <NativeSelect id="tax-year" className="w-full" value={taxYear} onChange={(event) => onChangeTaxYear(parseInt(event.target.value, 10))}>
          {years.map((year) => (
            <NativeSelectOption key={year} value={year}>
              {year}
            </NativeSelectOption>
          ))}
        </NativeSelect>
      </div>
    </div>
  );
}
