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
    <section className="panel">
      <div className="panel-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <ClipboardList size={32} color="var(--brand-primary)" />
          <h2>주문 관리</h2>
        </div>
        <p className="lead">
          계좌를 선택하고 주문을 생성하거나 주문 내역을 조회하고 취소할 수 있습니다.
        </p>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
        <span style={{ color: 'var(--text-secondary)', fontSize: '14px', minWidth: '48px' }}>계좌</span>
        <AccountSelector value={accountId} onChange={setAccountId} />
      </div>

      <div className="sub-tabs" style={{ marginBottom: '16px' }}>
        <button
          className={`sub-tab-btn ${activeTab === 'form' ? 'active' : ''}`}
          onClick={() => setActiveTab('form')}
        >
          주문 생성
        </button>
        <button
          className={`sub-tab-btn ${activeTab === 'list' ? 'active' : ''}`}
          onClick={() => setActiveTab('list')}
        >
          주문 내역
        </button>
      </div>

      {accountId === null ? (
        <p style={{ color: 'var(--text-secondary)', fontSize: '14px', textAlign: 'center', padding: '32px 0' }}>
          계좌를 선택해 주세요.
        </p>
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
