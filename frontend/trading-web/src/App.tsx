import { Suspense, lazy, useEffect, useState } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { HomePage } from "./pages/home/ui/HomePage";
import { Sidebar } from "./shared/ui/Sidebar";
import { TopBar } from "./shared/ui/TopBar";
import { ExecutionToastProvider } from "./features/execution-toast/ui/ExecutionToastProvider";
import { useToastStore } from "./features/execution-toast/model/useToastStore";
import { ToastContainer } from "./shared/ui/Toast";

const RealtimePage = lazy(() => import("./pages/realtime/ui/RealtimePage").then((m) => ({ default: m.RealtimePage })));
const HistoricalPage = lazy(() => import("./pages/historical/ui/HistoricalPage").then((m) => ({ default: m.HistoricalPage })));
const MacroPage = lazy(() => import("./pages/macro/ui/MacroPage").then((m) => ({ default: m.MacroPage })));
const MarketUnifiedChartPage = lazy(() =>
  import("./pages/market-unified/ui/MarketUnifiedChartPage").then((m) => ({ default: m.MarketUnifiedChartPage })),
);
const AccountDashboardPage = lazy(() =>
  import("./pages/account/ui/AccountDashboardPage").then((m) => ({ default: m.AccountDashboardPage })),
);
const OrderPage = lazy(() => import("./pages/order/ui/OrderPage").then((m) => ({ default: m.OrderPage })));
const PortfolioChartPage = lazy(() =>
  import("./pages/portfolio/ui/PortfolioChartPage").then((m) => ({ default: m.PortfolioChartPage })),
);
const TaxSummaryPage = lazy(() => import("./pages/tax-summary/ui/TaxSummaryPage").then((m) => ({ default: m.TaxSummaryPage })));
const TradingJournalPage = lazy(() =>
  import("./pages/trading-journal/ui/TradingJournalPage").then((m) => ({ default: m.TradingJournalPage })),
);

function App() {
  const [isSidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();
  const toasts = useToastStore((state) => state.toasts);
  const removeToast = useToastStore((state) => state.removeToast);

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth > 1024) setSidebarOpen(true);
      else setSidebarOpen(false);
    };
    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const getPageTitle = (path: string) => {
    switch (path) {
      case "/":
        return "대시보드";
      case "/realtime":
        return "실시간 데이터 수집";
      case "/historical":
        return "과거 OHLCV 수집";
      case "/macro":
        return "거시경제 데이터";
      case "/market-unified":
        return "통합 시세 차트";
      case "/account":
        return "계좌·포지션";
      case "/orders":
        return "주문 관리";
      case "/portfolio":
        return "포트폴리오 차트";
      case "/tax-summary":
        return "세금 요약";
      case "/trading-journals":
        return "거래 일지";
      default:
        return "Trading Console";
    }
  };

  return (
    <div className="flex min-h-screen w-screen bg-bg-main overflow-hidden">
      <ExecutionToastProvider />

      <Sidebar isOpen={isSidebarOpen} setOpen={setSidebarOpen} />

      <main className="flex-1 flex flex-col h-screen overflow-hidden relative">
        <TopBar title={getPageTitle(location.pathname)} toggleSidebar={() => setSidebarOpen(!isSidebarOpen)} />

        <div className="flex-1 overflow-y-auto px-4 py-5 sm:px-6 sm:py-6 lg:px-8 lg:py-8 flex flex-col gap-5 sm:gap-6 lg:gap-8 bg-[radial-gradient(circle_at_top_right,rgba(59,130,246,0.03),transparent_400px)]">
          <Suspense fallback={<div className="text-sm text-text-secondary">페이지 로딩 중...</div>}>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/realtime" element={<RealtimePage />} />
              <Route path="/historical" element={<HistoricalPage />} />
              <Route path="/macro" element={<MacroPage />} />
              <Route path="/market-unified" element={<MarketUnifiedChartPage />} />
              <Route path="/account" element={<AccountDashboardPage />} />
              <Route path="/orders" element={<OrderPage />} />
              <Route path="/portfolio" element={<PortfolioChartPage />} />
              <Route path="/tax-summary" element={<TaxSummaryPage />} />
              <Route path="/trading-journals" element={<TradingJournalPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </div>
      </main>

      <ToastContainer toasts={toasts} onDismiss={removeToast} />
    </div>
  );
}

export default App;
