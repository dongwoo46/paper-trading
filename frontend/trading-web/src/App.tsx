import { Suspense, lazy, useEffect, useState } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { HomePage } from "./pages/home/ui/HomePage";
import { Sidebar } from "./shared/ui/Sidebar";
import { TopBar } from "./shared/ui/TopBar";
import { ExecutionToastProvider } from "./features/execution-toast/ui/ExecutionToastProvider";
import { useToastStore } from "./features/execution-toast/model/useToastStore";
import { ToastContainer } from "./shared/ui/Toast";
import { subscribeAnalysisNotifications } from "./shared/api/chartAnalysisApi";
import { useNotificationStore } from "./shared/model/useNotificationStore";
import { Skeleton } from "./shared/ui/shadcn/skeleton";
import { RouteErrorBoundary } from "./shared/ui/RouteErrorPage";

const DESKTOP_BREAKPOINT_PX = 1024;

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
const ChartAnalysisPage = lazy(() =>
  import("./pages/chart-analysis/ui/ChartAnalysisPage").then((m) => ({ default: m.ChartAnalysisPage })),
);

function App() {
  const [isSidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();
  const toasts = useToastStore((state) => state.toasts);
  const removeToast = useToastStore((state) => state.removeToast);
  const addNotification = useNotificationStore((state) => state.addFromLlmDone);

  // 앱 전체 수명 동안 LLM 완료 알림 SSE 구독
  useEffect(() => {
    return subscribeAnalysisNotifications((event) => {
      addNotification(event);
    });
  }, [addNotification]);

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= DESKTOP_BREAKPOINT_PX) setSidebarOpen(true);
      else setSidebarOpen(false);
    };
    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  return (
    <div className="flex min-h-screen w-screen overflow-hidden bg-background">
      <ExecutionToastProvider />

      <Sidebar isOpen={isSidebarOpen} setOpen={setSidebarOpen} />

      <main className="relative flex h-screen min-w-0 flex-1 flex-col overflow-hidden">
        <TopBar toggleSidebar={() => setSidebarOpen((open) => !open)} />

        <div className="flex flex-1 overflow-y-auto bg-background">
          <div className="mx-auto flex w-full max-w-screen-2xl flex-col px-4 py-5 sm:px-6 sm:py-7 lg:px-8 lg:py-9">
            <RouteErrorBoundary resetKey={location.key}>
              <Suspense fallback={<Skeleton className="h-48 w-full" aria-label="페이지 로딩 중" />}>
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
                  <Route path="/chart-analysis" element={<ChartAnalysisPage />} />
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
              </Suspense>
            </RouteErrorBoundary>
          </div>
        </div>
      </main>

      <ToastContainer toasts={toasts} onDismiss={removeToast} />
    </div>
  );
}

export default App;
