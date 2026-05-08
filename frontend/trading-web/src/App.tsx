import { useEffect, useState } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AccountDashboardPage } from "./pages/account/ui/AccountDashboardPage";
import { HistoricalPage } from "./pages/historical/ui/HistoricalPage";
import { HomePage } from "./pages/home/ui/HomePage";
import { MacroPage } from "./pages/macro/ui/MacroPage";
import { MarketUnifiedChartPage } from "./pages/market-unified/ui/MarketUnifiedChartPage";
import { OrderPage } from "./pages/order/ui/OrderPage";
import { PortfolioChartPage } from "./pages/portfolio/ui/PortfolioChartPage";
import { RealtimePage } from "./pages/realtime/ui/RealtimePage";
import { TaxSummaryPage } from "./pages/tax-summary/ui/TaxSummaryPage";
import { TradingJournalPage } from "./pages/trading-journal/ui/TradingJournalPage";
import { Sidebar } from "./shared/ui/Sidebar";
import { TopBar } from "./shared/ui/TopBar";
import { ExecutionToastProvider } from "./features/execution-toast/ui/ExecutionToastProvider";
import { useToastStore } from "./features/execution-toast/model/useToastStore";
import { ToastContainer } from "./shared/ui/Toast";
import "./app/styles/App.css";
import "./shared/ui/Toast/toast.css";

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
    <div className="app-shell">
      <ExecutionToastProvider />

      <Sidebar isOpen={isSidebarOpen} setOpen={setSidebarOpen} />

      <main className="main-wrapper">
        <TopBar title={getPageTitle(location.pathname)} toggleSidebar={() => setSidebarOpen(!isSidebarOpen)} />

        <div className="page-content">
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
        </div>
      </main>

      <ToastContainer toasts={toasts} onDismiss={removeToast} />
    </div>
  );
}

export default App;
