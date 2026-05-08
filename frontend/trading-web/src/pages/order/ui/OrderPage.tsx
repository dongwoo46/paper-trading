import { useState } from 'react';
import { ClipboardList } from 'lucide-react';
import { AccountSelector } from '../../../features/order-form/ui/AccountSelector';
import { OrderFormPanel } from '../../../features/order-form/ui/OrderFormPanel';
import { OrderTablePanel } from '../../../features/order-list/ui/OrderTablePanel';

type ActiveTab = 'form' | 'list';

export function OrderPage() {
  const [accountId, setAccountId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<ActiveTab>('form');

  const handleFormSuccess = () => {
    setActiveTab('list');
  };

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <div className="flex items-center gap-4">
          <ClipboardList size={32} className="text-brand-primary" />
          <h2 className="text-[28px] font-bold tracking-tight">주문 관리</h2>
        </div>
        <p className="text-text-secondary text-[15px] max-w-3xl">
          계좌를 선택하고 주문을 생성하거나 주문 내역을 조회하고 취소할 수 있습니다.
        </p>
      </div>

      <div className="flex items-center gap-3 mb-4">
        <span className="text-text-secondary text-sm min-w-[48px]">계좌</span>
        <AccountSelector value={accountId} onChange={setAccountId} />
      </div>

      <div className="flex gap-2 p-1 bg-black/20 rounded-[16px] border border-white/12 w-fit mb-4">
        <button
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all ${activeTab === 'form' ? 'bg-blue-400/25 text-brand-primary' : 'text-text-muted hover:text-text-primary'}`}
          onClick={() => setActiveTab('form')}
        >
          주문 생성
        </button>
        <button
          className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all ${activeTab === 'list' ? 'bg-blue-400/25 text-brand-primary' : 'text-text-muted hover:text-text-primary'}`}
          onClick={() => setActiveTab('list')}
        >
          주문 내역
        </button>
      </div>

      {accountId === null ? (
        <p className="text-text-secondary text-sm text-center py-8">계좌를 선택해 주세요.</p>
      ) : (
        <>
          {activeTab === 'form' && (
            <OrderFormPanel accountId={accountId} onSuccess={handleFormSuccess} />
          )}
          {activeTab === 'list' && (
            <OrderTablePanel accountId={accountId} />
          )}
        </>
      )}
    </section>
  );
}
