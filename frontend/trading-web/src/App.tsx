import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import "./App.css";

type Mode = "paper" | "live";
type Subscriptions = Record<string, string[]>;
type KrSymbol = { symbol: string; name: string; market: string };

type ChangeResponse = {
  status: string;
  mode: string;
  symbol: string;
  totalRegistrations: number;
  maxRegistrations: number;
  subscriptions: Subscriptions;
};

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "";
const MODES: Mode[] = ["paper", "live"];

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
    ...init
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(`${response.status} ${response.statusText}: ${message}`);
  }
  return (await response.json()) as T;
}

function normalizeByModes(data: Subscriptions): Subscriptions {
  return {
    paper: data.paper ?? [],
    live: data.live ?? []
  };
}

function App() {
  const [wsSubscriptions, setWsSubscriptions] = useState<Subscriptions>(normalizeByModes({}));
  const [restWatchlist, setRestWatchlist] = useState<Subscriptions>(normalizeByModes({}));
  const [mode, setMode] = useState<Mode>("paper");
  const [symbol, setSymbol] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchMarket, setSearchMarket] = useState("");
  const [searchResults, setSearchResults] = useState<KrSymbol[]>([]);
  const [symbolNameMap, setSymbolNameMap] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("준비 완료");

  async function loadAll() {
    setLoading(true);
    try {
      const [ws, rest] = await Promise.all([
        fetchJson<Subscriptions>("/api/kis/ws/subscriptions"),
        fetchJson<Subscriptions>("/api/kis/rest/watchlist")
      ]);
      setWsSubscriptions(normalizeByModes(ws));
      setRestWatchlist(normalizeByModes(rest));
      setMessage("최신 상태를 불러왔습니다.");
    } catch (error) {
      setMessage(`조회 실패: ${(error as Error).message}`);
    } finally {
      setLoading(false);
    }
  }

  async function searchSymbols(query: string, market = searchMarket) {
    try {
      const params = new URLSearchParams();
      if (query.trim()) params.set("query", query.trim());
      if (market.trim()) params.set("market", market.trim());
      params.set("limit", "100");
      const result = await fetchJson<KrSymbol[]>(`/api/symbols/kr?${params.toString()}`);
      setSearchResults(result);
      setSymbolNameMap((prev) => {
        const next = { ...prev };
        for (const item of result) {
          next[item.symbol] = item.name;
        }
        return next;
      });
    } catch (error) {
      setMessage(`심볼 검색 실패: ${(error as Error).message}`);
    }
  }

  useEffect(() => {
    void loadAll();
    void searchSymbols("");
  }, []);

  async function applyWs(action: "add" | "remove") {
    if (!symbol.trim()) return;
    setLoading(true);
    try {
      const res = await fetchJson<ChangeResponse>("/api/kis/ws/subscriptions", {
        method: action === "add" ? "POST" : "DELETE",
        body: JSON.stringify({ mode, symbol: symbol.trim() })
      });
      setWsSubscriptions(normalizeByModes(res.subscriptions));
      setMessage(`WS ${action === "add" ? "추가" : "삭제"}: ${res.status} (${res.mode}:${res.symbol})`);
      setSymbol("");
    } catch (error) {
      setMessage(`WS ${action === "add" ? "추가" : "삭제"} 실패: ${(error as Error).message}`);
    } finally {
      setLoading(false);
    }
  }

  async function applyRest(action: "add" | "remove") {
    if (!symbol.trim()) return;
    setLoading(true);
    try {
      const res = await fetchJson<ChangeResponse>("/api/kis/rest/watchlist", {
        method: action === "add" ? "POST" : "DELETE",
        body: JSON.stringify({ mode, symbol: symbol.trim() })
      });
      setRestWatchlist(normalizeByModes(res.subscriptions));
      setMessage(`REST ${action === "add" ? "추가" : "삭제"}: ${res.status} (${res.mode}:${res.symbol})`);
      setSymbol("");
    } catch (error) {
      setMessage(`REST ${action === "add" ? "추가" : "삭제"} 실패: ${(error as Error).message}`);
    } finally {
      setLoading(false);
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault();
  }

  return (
    <div className="page">
      <header className="hero">
        <h1>트레이딩 웹 · 마켓 콜렉터</h1>
        <p>KIS WebSocket 구독과 REST 관심종목을 한 화면에서 운영합니다.</p>
        <button className="btn secondary" onClick={() => void loadAll()} disabled={loading}>
          {loading ? "새로고침 중..." : "새로고침"}
        </button>
      </header>

      <section className="control">
        <h2>국내 심볼 검색</h2>
        <div className="search-row">
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                void searchSymbols(searchQuery, searchMarket);
              }
            }}
            placeholder="종목코드 또는 종목명 검색"
            disabled={loading}
          />
          <select value={searchMarket} onChange={(e) => setSearchMarket(e.target.value)} disabled={loading}>
            <option value="">전체 시장</option>
            <option value="KOSPI">KOSPI</option>
            <option value="KOSDAQ">KOSDAQ</option>
          </select>
          <button className="btn secondary" type="button" onClick={() => void searchSymbols(searchQuery, searchMarket)} disabled={loading}>
            검색
          </button>
        </div>
        <div className="result-list">
          {searchResults.map((row) => (
            <button key={row.symbol} className="result-item" type="button" onClick={() => setSymbol(row.symbol)}>
              <span>{row.symbol}</span>
              <span>{row.name}</span>
              <span>{row.market}</span>
            </button>
          ))}
        </div>
      </section>

      <section className="control">
        <h2>구독/관심종목 관리</h2>
        <form onSubmit={onSubmit}>
          <label>
            모드
            <select value={mode} onChange={(e) => setMode(e.target.value as Mode)} disabled={loading}>
              <option value="paper">paper</option>
              <option value="live">live</option>
            </select>
          </label>
          <label>
            심볼
            <input
              value={symbol}
              onChange={(e) => setSymbol(e.target.value.toUpperCase())}
              placeholder="005930"
              disabled={loading}
            />
          </label>
          <div className="buttons">
            <button className="btn ws" type="button" onClick={() => void applyWs("add")} disabled={loading}>
              WS 추가
            </button>
            <button className="btn ws secondary" type="button" onClick={() => void applyWs("remove")} disabled={loading}>
              WS 삭제
            </button>
            <button className="btn rest" type="button" onClick={() => void applyRest("add")} disabled={loading}>
              REST 추가
            </button>
            <button className="btn rest secondary" type="button" onClick={() => void applyRest("remove")} disabled={loading}>
              REST 삭제
            </button>
          </div>
        </form>
      </section>

      <section className="grid">
        <article className="panel">
          <h2>WebSocket 구독 목록</h2>
          <ModeList data={wsSubscriptions} emptyMessage="WS 구독 심볼이 없습니다." symbolNameMap={symbolNameMap} />
        </article>
        <article className="panel">
          <h2>REST 관심종목</h2>
          <ModeList data={restWatchlist} emptyMessage="REST 관심종목이 없습니다." symbolNameMap={symbolNameMap} />
        </article>
      </section>

      <footer className="status">{message}</footer>
    </div>
  );
}

function ModeList({
  data,
  emptyMessage,
  symbolNameMap
}: {
  data: Subscriptions;
  emptyMessage: string;
  symbolNameMap: Record<string, string>;
}) {
  return (
    <div className="modes">
      {MODES.map((mode) => (
        <div className="mode" key={mode}>
          <h3>{mode}</h3>
          {data[mode]?.length ? (
            <div className="chips">
              {data[mode].map((s) => (
                <span key={`${mode}-${s}`} className="chip">
                  {s} {symbolNameMap[s] ? `· ${symbolNameMap[s]}` : ""}
                </span>
              ))}
            </div>
          ) : (
            <p className="empty">{emptyMessage}</p>
          )}
        </div>
      ))}
    </div>
  );
}

export default App;
