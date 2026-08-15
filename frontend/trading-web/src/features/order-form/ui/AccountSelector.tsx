import { useQuery } from '@tanstack/react-query';
import { fetchListAccounts } from '../../../entities/order/api/orderApi';
import { NativeSelect, NativeSelectOption } from '../../../shared/ui/shadcn/native-select';

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
    return (
      <NativeSelect id="account-selector" className="w-full sm:w-64" disabled>
        <NativeSelectOption>로딩 중...</NativeSelectOption>
      </NativeSelect>
    );
  }

  if (isError) {
    return (
      <NativeSelect id="account-selector" className="w-full sm:w-64" disabled>
        <NativeSelectOption>계좌 목록 조회 실패</NativeSelectOption>
      </NativeSelect>
    );
  }

  return (
    <NativeSelect
      id="account-selector"
      className="w-full sm:w-64"
      value={value ?? ''}
      onChange={(e) => {
        const id = parseInt(e.target.value, 10);
        if (!isNaN(id)) onChange(id);
      }}
    >
      <NativeSelectOption value="">계좌 선택</NativeSelectOption>
      {accounts.map((account) => (
        <NativeSelectOption
          key={account.id}
          value={account.id}
          disabled={!account.isActive}
        >
          {account.accountName} ({account.tradingMode}){!account.isActive ? ' — 비활성' : ''}
        </NativeSelectOption>
      ))}
    </NativeSelect>
  );
}
