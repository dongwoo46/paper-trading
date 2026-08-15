import { useState } from 'react';
import { AccountSelector } from '../../../features/order-form/ui/AccountSelector';
import { OrderFormPanel } from '../../../features/order-form/ui/OrderFormPanel';
import { OrderTablePanel } from '../../../features/order-list/ui/OrderTablePanel';
import { Card, CardContent } from '../../../shared/ui/shadcn/card';
import { Label } from '../../../shared/ui/shadcn/label';
import { Tabs, TabsList, TabsTrigger } from '../../../shared/ui/shadcn/tabs';
import { PageHeader } from '../../../shared/ui/PageHeader';

type ActiveTab = 'form' | 'list';

export function OrderPage() {
  const [accountId, setAccountId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<ActiveTab>('form');

  const handleFormSuccess = () => {
    setActiveTab('list');
  };

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <PageHeader
        eyebrow="트레이딩"
        title="주문 관리"
        description="계좌를 선택해 주문을 생성하고 기존 주문을 조회하거나 취소합니다."
      />

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <Label htmlFor="account-selector" className="sm:min-w-12">계좌</Label>
        <AccountSelector value={accountId} onChange={setAccountId} />
      </div>

      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as ActiveTab)}>
        <TabsList aria-label="주문 화면">
          <TabsTrigger value="form">주문 생성</TabsTrigger>
          <TabsTrigger value="list">주문 내역</TabsTrigger>
        </TabsList>
      </Tabs>

      <Card>
        <CardContent>
          {accountId === null ? (
            <p className="py-8 text-center text-sm text-muted-foreground">계좌를 선택해 주세요.</p>
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
        </CardContent>
      </Card>
    </section>
  );
}
