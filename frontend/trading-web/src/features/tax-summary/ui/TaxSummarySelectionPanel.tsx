import type { AccountResponse } from "../../../entities/account/model/types";

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
    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "12px", marginBottom: "16px" }}>
      <label style={{ display: "flex", flexDirection: "column", gap: "6px", fontSize: "14px" }}>
        계좌 선택
        <select value={accountId} onChange={(event) => onChangeAccountId(parseInt(event.target.value, 10))}>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.accountName}
            </option>
          ))}
        </select>
      </label>
      <label style={{ display: "flex", flexDirection: "column", gap: "6px", fontSize: "14px" }}>
        연도 선택
        <select value={taxYear} onChange={(event) => onChangeTaxYear(parseInt(event.target.value, 10))}>
          {years.map((year) => (
            <option key={year} value={year}>
              {year}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
