import { useEffect, useState } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { HistoricalPage } from "./pages/historical/ui/HistoricalPage";
import { HomePage } from "./pages/home/ui/HomePage";
import { MacroPage } from "./pages/macro/ui/MacroPage";
import { RealtimePage } from "./pages/realtime/ui/RealtimePage";
import { Sidebar } from "./shared/ui/Sidebar";
import { TopBar } from "./shared/ui/TopBar";
import "./app/styles/App.css";

function App() {
  const [isSidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();

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
      default:
        return "Trading Console";
    }
  };

  return (
    <div className="app-shell">
      <Sidebar isOpen={isSidebarOpen} setOpen={setSidebarOpen} />

      <main className="main-wrapper">
        <TopBar title={getPageTitle(location.pathname)} toggleSidebar={() => setSidebarOpen(!isSidebarOpen)} />

        <div className="page-content">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/realtime" element={<RealtimePage />} />
            <Route path="/historical" element={<HistoricalPage />} />
            <Route path="/macro" element={<MacroPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </main>
    </div>
  );
}

export default App;
