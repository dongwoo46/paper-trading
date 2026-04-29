import { useQuery } from '@tanstack/react-query';
import { fetchListAccounts } from '../../../entities/order/api/orderApi';

interface AccountSelectorProps {
  value: number | null;
  onChange: (accountId: number) => void;
}

export function AccountSelector({ value, onChange }: AccountSelectorProps) {
  const { data: accounts = [], isLoading, isError } = useQuery({
    queryKey: ['accounts'],
    queryFn: fetchListAccounts,
  });

  if (isLoading) {
    return <select disabled><option>로딩 중...</option></select>;
  }

  if (isError) {
    return <select disabled><option>계좌 목록 조회 실패</option></select>;
  }

  return (
    <select
      value={value ?? ''}
      onChange={(e) => {
        const id = parseInt(e.target.value, 10);
        if (!isNaN(id)) onChange(id);
      }}
    >
      <option value="">계좌 선택</option>
      {accounts.map((account) => (
        <option
          key={account.id}
          value={account.id}
          disabled={!account.isActive}
        >
          {account.accountName} ({account.tradingMode}){!account.isActive ? ' — 비활성' : ''}
        </option>
      ))}
    </select>
  );
}
