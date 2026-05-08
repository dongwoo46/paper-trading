import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type MouseEventParams,
  type Time,
} from "lightweight-charts";
import {
  fetchDailyBars,
  fetchMarketSymbols,
  fetchMinuteBars,
  fetchWeeklyBars,
  type MarketSource,
  type OhlcvPoint,
  type UnifiedInterval,
} from "../../../shared/api/marketUnifiedApi";
import "./MarketUnifiedChartPage.css";

const INTERVAL_LABEL: Record<UnifiedInterval, string> = {
  "1m": "1분봉",
  "5m": "5분봉",
  "10m": "10분봉",
  "1d": "일봉",
  "1w": "주봉",
};

function formatNumber(value: number): string {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(value);
}

type CrosshairInfo = {
  startedAt: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};

type DrawTool = "none" | "trend" | "hline";

type DrawPoint = {
  logical: number;
  price: number;
};

type TrendLine = {
  id: string;
  p1: DrawPoint;
  p2: DrawPoint;
};

type HorizontalLine = {
  id: string;
  price: number;
};

function toChartTime(startedAt: string): Time {
  const normalized = startedAt.replace(" ", "T");
  const d = new Date(normalized);
  return Math.floor(d.getTime() / 1000) as Time;
}

function UnifiedCandlestickChart({
  bars,
  drawTool,
  onCrosshairChange,
}: {
  bars: OhlcvPoint[];
  drawTool: DrawTool;
  onCrosshairChange: (value: CrosshairInfo | null) => void;
}) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
  const [trendLines, setTrendLines] = useState<TrendLine[]>([]);
  const [hLines, setHLines] = useState<HorizontalLine[]>([]);
  const [pendingTrendStart, setPendingTrendStart] = useState<DrawPoint | null>(null);

  const overlay = useMemo(() => {
    const chart = chartRef.current;
    const series = seriesRef.current;
    if (!chart || !series) return { trendSegments: [], hSegments: [] as Array<{ id: string; y: number }> };

    const trendSegments = trendLines
      .map((line) => {
        const x1 = chart.timeScale().logicalToCoordinate(line.p1.logical);
        const x2 = chart.timeScale().logicalToCoordinate(line.p2.logical);
        const y1 = series.priceToCoordinate(line.p1.price);
        const y2 = series.priceToCoordinate(line.p2.price);
        if (x1 == null || x2 == null || y1 == null || y2 == null) return null;
        return { id: line.id, x1, y1, x2, y2 };
      })
      .filter((v): v is { id: string; x1: number; y1: number; x2: number; y2: number } => Boolean(v));

    const hSegments = hLines
      .map((line) => {
        const y = series.priceToCoordinate(line.price);
        if (y == null) return null;
        return { id: line.id, y };
      })
      .filter((v): v is { id: string; y: number } => Boolean(v));

    return { trendSegments, hSegments };
  }, [trendLines, hLines, bars]);

  useEffect(() => {
    if (!rootRef.current) return;
    const root = rootRef.current;
    const chart = createChart(root, {
      width: root.clientWidth,
      height: 360,
      layout: {
        background: { type: ColorType.Solid, color: "#0b1220" },
        textColor: "#d1d5db",
      },
      grid: {
        vertLines: { color: "rgba(255,255,255,0.06)" },
        horzLines: { color: "rgba(255,255,255,0.06)" },
      },
      crosshair: { mode: CrosshairMode.Normal },
      rightPriceScale: {
        borderColor: "rgba(255,255,255,0.2)",
      },
      timeScale: {
        borderColor: "rgba(255,255,255,0.2)",
        timeVisible: true,
        secondsVisible: false,
      },
      handleScroll: {
        mouseWheel: true,
        pressedMouseMove: true,
        horzTouchDrag: true,
        vertTouchDrag: true,
      },
      handleScale: {
        mouseWheel: true,
        pinch: true,
        axisPressedMouseMove: true,
      },
    });

    const series = chart.addSeries(CandlestickSeries, {
      upColor: "#22c55e",
      downColor: "#ef4444",
      borderVisible: true,
      wickUpColor: "#22c55e",
      wickDownColor: "#ef4444",
    });

    chartRef.current = chart;
    seriesRef.current = series;

    const onResize = () => {
      chart.applyOptions({ width: root.clientWidth });
    };
    const ro = new ResizeObserver(onResize);
    ro.observe(root);

    return () => {
      ro.disconnect();
      chart.remove();
      chartRef.current = null;
      seriesRef.current = null;
    };
  }, []);

  useEffect(() => {
    const chart = chartRef.current;
    const series = seriesRef.current;
    if (!chart || !series) return;

    const data = bars.map((bar) => ({
      time: toChartTime(bar.startedAt),
      open: bar.open,
      high: bar.high,
      low: bar.low,
      close: bar.close,
    }));
    series.setData(data);
    chart.timeScale().fitContent();
  }, [bars]);

  useEffect(() => {
    const chart = chartRef.current;
    const series = seriesRef.current;
    if (!chart || !series) return;

    const onMove = (param: MouseEventParams<Time>) => {
      const point = param.seriesData.get(series);
      if (!point || !("open" in point)) {
        onCrosshairChange(null);
        return;
      }

      const time = typeof param.time === "number" ? param.time : null;
      const startedAt = time != null ? new Date(time * 1000).toISOString().replace("T", " ").slice(0, 19) : "-";
      const index = bars.findIndex((b) => toChartTime(b.startedAt) === param.time);
      const volume = index >= 0 ? bars[index].volume : 0;

      onCrosshairChange({
        startedAt,
        open: point.open,
        high: point.high,
        low: point.low,
        close: point.close,
        volume,
      });
    };

    const onClick = (param: MouseEventParams<Time>) => {
      if (drawTool === "none" || !param.point) return;
      const logical = chart.timeScale().coordinateToLogical(param.point.x);
      const price = series.coordinateToPrice(param.point.y);
      if (logical == null || price == null) return;

      if (drawTool === "hline") {
        setHLines((prev) => [...prev, { id: crypto.randomUUID(), price }]);
        return;
      }

      if (!pendingTrendStart) {
        setPendingTrendStart({ logical, price });
        return;
      }

      setTrendLines((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          p1: pendingTrendStart,
          p2: { logical, price },
        },
      ]);
      setPendingTrendStart(null);
    };

    const onRangeChanged = () => {
      setTrendLines((prev) => [...prev]);
      setHLines((prev) => [...prev]);
    };

    chart.subscribeCrosshairMove(onMove);
    chart.subscribeClick(onClick);
    chart.timeScale().subscribeVisibleLogicalRangeChange(onRangeChanged);

    return () => {
      chart.unsubscribeCrosshairMove(onMove);
      chart.unsubscribeClick(onClick);
      chart.timeScale().unsubscribeVisibleLogicalRangeChange(onRangeChanged);
    };
  }, [bars, drawTool, pendingTrendStart, onCrosshairChange]);

  useEffect(() => {
    if (drawTool !== "trend") {
      setPendingTrendStart(null);
    }
  }, [drawTool]);

  return (
    <div className="lw-chart-shell">
      <div className="lw-chart-root" ref={rootRef} />
      <svg className="lw-chart-overlay" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
        {overlay.hSegments.map((line) => (
          <line key={line.id} x1="0" y1={(line.y / 360) * 100} x2="100" y2={(line.y / 360) * 100} className="draw-line draw-line-h" />
        ))}
        {overlay.trendSegments.map((line) => (
          <line
            key={line.id}
            x1={(line.x1 / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100}
            y1={(line.y1 / 360) * 100}
            x2={(line.x2 / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100}
            y2={(line.y2 / 360) * 100}
            className="draw-line draw-line-trend"
          />
        ))}
      </svg>
      {(trendLines.length > 0 || hLines.length > 0) && (
        <button
          type="button"
          className="btn btn-outline chart-clear-btn"
          onClick={() => {
            setTrendLines([]);
            setHLines([]);
            setPendingTrendStart(null);
          }}
        >
          선 전체 삭제
        </button>
      )}
    </div>
  );
}

export function MarketUnifiedChartPage() {
  const [source, setSource] = useState<MarketSource>("yfinance");
  const [keyword, setKeyword] = useState("");
  const [selectedSymbol, setSelectedSymbol] = useState("AAPL");
  const [interval, setInterval] = useState<UnifiedInterval>("1d");
  const [minuteLimit, setMinuteLimit] = useState(100);
  const [drawTool, setDrawTool] = useState<DrawTool>("none");
  const [crosshairInfo, setCrosshairInfo] = useState<CrosshairInfo | null>(null);

  const symbolsQuery = useQuery({
    queryKey: ["market-symbols", source],
    queryFn: () => fetchMarketSymbols(source, 300),
  });

  const filteredSymbols = useMemo(() => {
    const list = symbolsQuery.data ?? [];
    const q = keyword.trim().toLowerCase();
    if (!q) return list.slice(0, 80);
    return list.filter((item) => item.symbol.toLowerCase().includes(q) || item.name.toLowerCase().includes(q)).slice(0, 80);
  }, [symbolsQuery.data, keyword]);

  const barsQuery = useQuery({
    queryKey: ["market-unified-bars", source, selectedSymbol, interval, minuteLimit],
    enabled: selectedSymbol.trim().length > 0,
    queryFn: async () => {
      if (interval === "1d") {
        return fetchDailyBars(source, selectedSymbol);
      }
      if (interval === "1w") {
        return fetchWeeklyBars(source, selectedSymbol, 130);
      }
      return fetchMinuteBars(selectedSymbol, interval, minuteLimit);
    },
  });

  const bars = barsQuery.data ?? [];
  const last = bars[bars.length - 1];

  return (
    <section className="panel">
      <div className="panel-header">
        <h2>통합 시세 차트</h2>
        <p className="lead">종목 선택 후 주봉/일봉/1·5·10분봉을 한 화면에서 전환해서 확인합니다.</p>
      </div>

      <div className="market-unified-layout">
        <div className="market-unified-left">
          <div className="section-card" style={{ padding: 16 }}>
            <div className="market-unified-controls">
              <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="종목 검색 (예: AAPL, 005930)" />
              <select value={source} onChange={(e) => setSource(e.target.value as MarketSource)}>
                <option value="yfinance">yfinance</option>
                <option value="pykrx">pykrx</option>
              </select>
              <button className="btn btn-outline" onClick={() => symbolsQuery.refetch()}>
                새로고침
              </button>
            </div>
          </div>

          <div className="section-card">
            <div className="section-card-header">
              <h3>종목 테이블</h3>
            </div>
            <div className="market-unified-symbol-table">
              <table>
                <thead>
                  <tr>
                    <th>심볼</th>
                    <th>이름</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSymbols.map((item) => (
                    <tr
                      key={`${item.symbol}-${item.name}`}
                      className={`market-unified-symbol-row ${selectedSymbol === item.symbol ? "active" : ""}`}
                      onClick={() => setSelectedSymbol(item.symbol)}
                    >
                      <td>{item.symbol}</td>
                      <td>{item.name}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div className="market-unified-right">
          <div className="section-card" style={{ padding: 16 }}>
            <div className="market-unified-tabs">
              {(["1w", "1d", "1m", "5m", "10m"] as UnifiedInterval[]).map((v) => (
                <button
                  key={v}
                  className={`tab-btn ${interval === v ? "active" : ""}`}
                  onClick={() => setInterval(v)}
                  type="button"
                >
                  {INTERVAL_LABEL[v]}
                </button>
              ))}
              {(interval === "1m" || interval === "5m" || interval === "10m") && (
                <select aria-label="분봉 조회 개수" value={minuteLimit} onChange={(e) => setMinuteLimit(Number(e.target.value))}>
                  <option value={50}>50개</option>
                  <option value={100}>100개</option>
                </select>
              )}
            </div>
            <div className="market-unified-draw-tools">
              <button type="button" className={`btn btn-outline ${drawTool === "none" ? "active-tool" : ""}`} onClick={() => setDrawTool("none")}>
                선택
              </button>
              <button type="button" className={`btn btn-outline ${drawTool === "trend" ? "active-tool" : ""}`} onClick={() => setDrawTool("trend")}>
                추세선
              </button>
              <button type="button" className={`btn btn-outline ${drawTool === "hline" ? "active-tool" : ""}`} onClick={() => setDrawTool("hline")}>
                수평선
              </button>
            </div>
            <div className="market-unified-meta">
              <span>선택 종목: {selectedSymbol || "-"}</span>
              <span>주기: {INTERVAL_LABEL[interval]}</span>
              <span>데이터: {bars.length}건</span>
              {last && <span>최신 종가: {formatNumber(last.close)}</span>}
              {crosshairInfo && (
                <>
                  <span>포인트: {crosshairInfo.startedAt}</span>
                  <span>O: {formatNumber(crosshairInfo.open)}</span>
                  <span>H: {formatNumber(crosshairInfo.high)}</span>
                  <span>L: {formatNumber(crosshairInfo.low)}</span>
                  <span>C: {formatNumber(crosshairInfo.close)}</span>
                  <span>V: {formatNumber(crosshairInfo.volume)}</span>
                </>
              )}
            </div>
          </div>

          <div className="market-unified-chart">
            {barsQuery.isLoading && <p>차트 데이터를 불러오는 중...</p>}
            {barsQuery.isError && <p>차트 조회 중 오류가 발생했습니다.</p>}
            {!barsQuery.isLoading && !barsQuery.isError && bars.length === 0 && <p>데이터가 없습니다.</p>}
            {!barsQuery.isLoading && !barsQuery.isError && bars.length > 0 && (
              <UnifiedCandlestickChart bars={bars} drawTool={drawTool} onCrosshairChange={setCrosshairInfo} />
            )}
          </div>

          <div className="section-card">
            <div className="section-card-header">
              <h3>OHLCV 테이블</h3>
            </div>
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>시각/일자</th>
                    <th>시가</th>
                    <th>고가</th>
                    <th>저가</th>
                    <th>종가</th>
                    <th>거래량</th>
                  </tr>
                </thead>
                <tbody>
                  {bars.slice().reverse().map((bar) => (
                    <tr key={`${bar.startedAt}-${bar.close}`}>
                      <td>{bar.startedAt}</td>
                      <td>{formatNumber(bar.open)}</td>
                      <td>{formatNumber(bar.high)}</td>
                      <td>{formatNumber(bar.low)}</td>
                      <td>{formatNumber(bar.close)}</td>
                      <td>{formatNumber(bar.volume)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
