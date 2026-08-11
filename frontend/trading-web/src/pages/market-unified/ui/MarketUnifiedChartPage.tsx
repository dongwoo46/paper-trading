import { useEffect, useMemo, useRef, useState, type RefObject } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  createChart,
  HistogramSeries,
  LineSeries,
  LineStyle,
  type IChartApi,
  type ISeriesApi,
  type Logical,
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
import {
  fetchInvestorFlow,
  type InvestorFlowPoint,
} from "../../../shared/api/investorFlowApi";
import { useChartDrawStore } from "../../../features/chart-drawing/model/useChartDrawStore";
import type {
  TrendLine,
  HorizontalLine,
  CrossLine,
} from "../../../features/chart-drawing/model/useChartDrawStore";
import { useIndicatorStore } from "../../../features/unified-chart-indicators/model/useIndicatorStore";
import {
  calcBollingerBands,
  calcRsi,
  calcMacd,
  calcMA,
  type OhlcvInput,
} from "../../../features/unified-chart-indicators/model/indicators";
import { IndicatorToggleBar } from "../../../features/unified-chart-indicators/ui/IndicatorToggleBar";
import { Alert, AlertDescription } from "@/shared/ui/shadcn/alert";
import { Badge } from "@/shared/ui/shadcn/badge";
import { Button } from "@/shared/ui/shadcn/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/shadcn/card";
import { Input } from "@/shared/ui/shadcn/input";
import { NativeSelect, NativeSelectOption } from "@/shared/ui/shadcn/native-select";
import { Skeleton } from "@/shared/ui/shadcn/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/ui/shadcn/table";

const INTERVAL_LABEL: Record<UnifiedInterval, string> = {
  "1m": "1분봉",
  "5m": "5분봉",
  "10m": "10분봉",
  "1d": "일봉",
  "1w": "주봉",
};

type ChartTheme = {
  background: string;
  foreground: string;
  grid: string;
  border: string;
  up: string;
  down: string;
  ma5: string;
  ma20: string;
  ma60: string;
  ma120: string;
  band: string;
  bandMiddle: string;
  rsi: string;
  cross: string;
};

function readChartTheme(root: HTMLElement): ChartTheme {
  const styles = getComputedStyle(root);
  const token = (name: string) => styles.getPropertyValue(name).trim();

  return {
    background: token("--chart-background"),
    foreground: token("--chart-foreground"),
    grid: token("--chart-grid"),
    border: token("--chart-border"),
    up: token("--chart-up"),
    down: token("--chart-down"),
    ma5: token("--chart-ma5"),
    ma20: token("--chart-ma20"),
    ma60: token("--chart-ma60"),
    ma120: token("--chart-ma120"),
    band: token("--chart-band"),
    bandMiddle: token("--chart-band-middle"),
    rsi: token("--chart-rsi"),
    cross: token("--chart-cross"),
  };
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(
    value,
  );
}

type IndicatorSnapshot = {
  ma5?: number;
  ma20?: number;
  ma60?: number;
  ma120?: number;
  bbUpper?: number;
  bbMiddle?: number;
  bbLower?: number;
  rsi?: number;
  macd?: number;
  macdSignal?: number;
  macdHist?: number;
};

type CrosshairInfo = {
  startedAt: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  indicators: IndicatorSnapshot;
};

type DrawTool = "none" | "trend" | "hline" | "cross";

type DrawPoint = {
  logical: number;
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
  chartKey,
  interval,
  source,
  symbol,
  onCrosshairChange,
}: {
  bars: OhlcvPoint[];
  drawTool: DrawTool;
  chartKey: string;
  interval: UnifiedInterval;
  source: MarketSource;
  symbol: string;
  onCrosshairChange: (value: CrosshairInfo | null) => void;
}) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
  const volSeriesRef = useRef<ISeriesApi<"Histogram"> | null>(null);

  const rsiSeriesRef = useRef<ISeriesApi<"Line"> | null>(null);
  const macdLineRef = useRef<ISeriesApi<"Line"> | null>(null);
  const macdSignalRef = useRef<ISeriesApi<"Line"> | null>(null);
  const macdHistRef = useRef<ISeriesApi<"Histogram"> | null>(null);
  const bbUpperRef = useRef<ISeriesApi<"Line"> | null>(null);
  const bbMiddleRef = useRef<ISeriesApi<"Line"> | null>(null);
  const bbLowerRef = useRef<ISeriesApi<"Line"> | null>(null);
  const ma5Ref = useRef<ISeriesApi<"Line"> | null>(null);
  const ma20Ref = useRef<ISeriesApi<"Line"> | null>(null);
  const ma60Ref = useRef<ISeriesApi<"Line"> | null>(null);
  const ma120Ref = useRef<ISeriesApi<"Line"> | null>(null);

  const flowForeignRef = useRef<ISeriesApi<"Line"> | null>(null);
  const flowInstRef = useRef<ISeriesApi<"Line"> | null>(null);
  const flowIndivRef = useRef<ISeriesApi<"Line"> | null>(null);

  const trendLines = useChartDrawStore((s) => s.trendLines[chartKey] ?? []);
  const hLines = useChartDrawStore((s) => s.hLines[chartKey] ?? []);
  const crossLines = useChartDrawStore((s) => s.crossLines[chartKey] ?? []);
  const addTrendLine = useChartDrawStore((s) => s.addTrendLine);
  const addHLine = useChartDrawStore((s) => s.addHLine);
  const addCrossLine = useChartDrawStore((s) => s.addCrossLine);
  const clearLines = useChartDrawStore((s) => s.clearLines);

  const [pendingTrendStart, setPendingTrendStart] = useState<DrawPoint | null>(
    null,
  );

  const [, forceUpdate] = useState(0);

  const active = useIndicatorStore((s) => s.active);

  const investorFlowActive = active.investorFlow && source === "pykrx" && interval === "1d";

  const dateFrom = bars.length > 0 ? bars[0].startedAt.slice(0, 10) : undefined;
  const dateTo = bars.length > 0 ? bars[bars.length - 1].startedAt.slice(0, 10) : undefined;

  const flowQuery = useQuery({
    queryKey: ["investor-flow", bars[0]?.startedAt, bars[bars.length - 1]?.startedAt, chartKey],
    enabled: investorFlowActive && bars.length > 0,
    queryFn: () => fetchInvestorFlow(symbol, dateFrom, dateTo),
  });

  const [chartHeight, setChartHeight] = useState(480);

  const ohlcvInputs: OhlcvInput[] = useMemo(
    () =>
      bars.map((b) => ({
        time: b.startedAt.slice(0, 10),
        open: b.open,
        high: b.high,
        low: b.low,
        close: b.close,
        volume: b.volume,
      })),
    [bars],
  );

  const overlay = useMemo(() => {
    const chart = chartRef.current;
    const series = seriesRef.current;
    if (!chart || !series)
      return {
        trendSegments: [],
        hSegments: [] as Array<{ id: string; y: number }>,
        crossSegments: [] as Array<{ id: string; x: number; y: number }>,
      };

    const trendSegments = trendLines
      .map((line: TrendLine) => {
        const x1 = chart
          .timeScale()
          .logicalToCoordinate(line.p1.logical as unknown as Logical);
        const x2 = chart
          .timeScale()
          .logicalToCoordinate(line.p2.logical as unknown as Logical);
        const y1 = series.priceToCoordinate(line.p1.price);
        const y2 = series.priceToCoordinate(line.p2.price);
        if (x1 == null || x2 == null || y1 == null || y2 == null) return null;
        return {
          id: line.id,
          x1: Number(x1),
          y1: Number(y1),
          x2: Number(x2),
          y2: Number(y2),
        };
      })
      .filter((v) => v !== null);

    const hSegments = hLines
      .map((line: HorizontalLine) => {
        const y = series.priceToCoordinate(line.price);
        if (y == null) return null;
        return { id: line.id, y: Number(y) };
      })
      .filter((v) => v !== null);

    const crossSegments = crossLines
      .map((line: CrossLine) => {
        const x = chart.timeScale().logicalToCoordinate(line.logical as unknown as Logical);
        const y = series.priceToCoordinate(line.price);
        if (x == null || y == null) return null;
        return { id: line.id, x: Number(x), y: Number(y) };
      })
      .filter((v) => v !== null);

    return { trendSegments, hSegments, crossSegments };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trendLines, hLines, crossLines, bars]);

  useEffect(() => {
    if (!rootRef.current) return;
    const root = rootRef.current;

    const rsiActive = active.rsi && interval === "1d";
    const macdActive = active.macd && interval === "1d";
    const bbActive = active.bb && interval === "1d";
    const investorFlowActive =
      active.investorFlow && source === "pykrx" && interval === "1d";
    const computedHeight =
      480 +
      (rsiActive ? 120 : 0) +
      (macdActive ? 120 : 0) +
      (investorFlowActive ? 120 : 0);

    setChartHeight(computedHeight);
    const theme = readChartTheme(root);

    const chart = createChart(root, {
      width: root.clientWidth,
      height: computedHeight,
      layout: {
        background: { type: ColorType.Solid, color: theme.background },
        textColor: theme.foreground,
      },
      grid: {
        vertLines: { color: theme.grid },
        horzLines: { color: theme.grid },
      },
      crosshair: { mode: CrosshairMode.Normal },
      rightPriceScale: {
        borderColor: theme.border,
      },
      timeScale: {
        borderColor: theme.border,
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
      upColor: theme.up,
      downColor: theme.down,
      borderVisible: true,
      wickUpColor: theme.up,
      wickDownColor: theme.down,
    });

    const volSeries = chart.addSeries(
      HistogramSeries,
      {
        priceScaleId: "vol",
        priceFormat: { type: "volume" },
        visible: active.volume,
      },
      1,
    );

    chart.panes()[0].setStretchFactor(3);
    chart.panes()[1].setStretchFactor(1);

    chart.priceScale("vol").applyOptions({
      scaleMargins: { top: 0.1, bottom: 0 },
    });

    if (active.ma5) {
      const ma5 = chart.addSeries(
        LineSeries,
        { color: theme.ma5, lineWidth: 1, priceLineVisible: false },
        0,
      );
      ma5Ref.current = ma5;
    }
    if (active.ma20) {
      const ma20 = chart.addSeries(
        LineSeries,
        { color: theme.ma20, lineWidth: 1, priceLineVisible: false },
        0,
      );
      ma20Ref.current = ma20;
    }
    if (active.ma60) {
      const ma60 = chart.addSeries(
        LineSeries,
        { color: theme.ma60, lineWidth: 1, priceLineVisible: false },
        0,
      );
      ma60Ref.current = ma60;
    }
    if (active.ma120) {
      const ma120 = chart.addSeries(
        LineSeries,
        { color: theme.ma120, lineWidth: 1, priceLineVisible: false },
        0,
      );
      ma120Ref.current = ma120;
    }

    if (bbActive) {
      const bbUpper = chart.addSeries(
        LineSeries,
        { color: theme.band, lineWidth: 1, priceLineVisible: false },
        0,
      );
      const bbMiddle = chart.addSeries(
        LineSeries,
        {
          color: theme.bandMiddle,
          lineWidth: 1,
          lineStyle: LineStyle.Dashed,
          priceLineVisible: false,
        },
        0,
      );
      const bbLower = chart.addSeries(
        LineSeries,
        { color: theme.band, lineWidth: 1, priceLineVisible: false },
        0,
      );
      bbUpperRef.current = bbUpper;
      bbMiddleRef.current = bbMiddle;
      bbLowerRef.current = bbLower;
    }

    const rsiPaneIndex = 2;
    if (rsiActive) {
      const rsiSeries = chart.addSeries(
        LineSeries,
        { color: theme.rsi, lineWidth: 1 },
        rsiPaneIndex,
      );
      rsiSeries.createPriceLine({
        price: 70,
        color: theme.down,
        lineWidth: 1,
        lineStyle: LineStyle.Dashed,
        axisLabelVisible: true,
        title: "",
      });
      rsiSeries.createPriceLine({
        price: 30,
        color: theme.up,
        lineWidth: 1,
        lineStyle: LineStyle.Dashed,
        axisLabelVisible: true,
        title: "",
      });
      chart.panes()[rsiPaneIndex].setStretchFactor(1);
      rsiSeriesRef.current = rsiSeries;
    }

    if (macdActive) {
      const macdPaneIndex = rsiActive ? 3 : 2;
      const macdLine = chart.addSeries(
        LineSeries,
        { color: theme.ma20, lineWidth: 1 },
        macdPaneIndex,
      );
      const macdSignal = chart.addSeries(
        LineSeries,
        { color: theme.ma60, lineWidth: 1 },
        macdPaneIndex,
      );
      const macdHist = chart.addSeries(
        HistogramSeries,
        {},
        macdPaneIndex,
      );
      chart.panes()[macdPaneIndex].setStretchFactor(1);
      macdLineRef.current = macdLine;
      macdSignalRef.current = macdSignal;
      macdHistRef.current = macdHist;
    }

    if (investorFlowActive) {
      const flowPaneIndex = 2 + (rsiActive ? 1 : 0) + (macdActive ? 1 : 0);
      const flowForeign = chart.addSeries(LineSeries, {
        color: theme.ma20,
        lineWidth: 1,
        priceLineVisible: false,
        title: "외국인",
      }, flowPaneIndex);
      const flowInst = chart.addSeries(LineSeries, {
        color: theme.up,
        lineWidth: 1,
        priceLineVisible: false,
        title: "기관",
      }, flowPaneIndex);
      const flowIndiv = chart.addSeries(LineSeries, {
        color: theme.down,
        lineWidth: 1,
        priceLineVisible: false,
        title: "개인",
      }, flowPaneIndex);
      chart.panes()[flowPaneIndex].setStretchFactor(1);
      flowForeignRef.current = flowForeign;
      flowInstRef.current = flowInst;
      flowIndivRef.current = flowIndiv;
    }

    chartRef.current = chart;
    seriesRef.current = series;
    volSeriesRef.current = volSeries;

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
      volSeriesRef.current = null;
      rsiSeriesRef.current = null;
      macdLineRef.current = null;
      macdSignalRef.current = null;
      macdHistRef.current = null;
      bbUpperRef.current = null;
      bbMiddleRef.current = null;
      bbLowerRef.current = null;
      ma5Ref.current = null;
      ma20Ref.current = null;
      ma60Ref.current = null;
      ma120Ref.current = null;
      flowForeignRef.current = null;
      flowInstRef.current = null;
      flowIndivRef.current = null;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active, source]);

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

    const theme = rootRef.current ? readChartTheme(rootRef.current) : null;
    const volData = bars.map((bar) => ({
      time: toChartTime(bar.startedAt),
      value: bar.volume,
      color: bar.close >= bar.open ? theme?.up : theme?.down,
    }));
    volSeriesRef.current?.setData(volData);

    if (ma5Ref.current)
      ma5Ref.current.setData(
        calcMA(ohlcvInputs, 5).map((p) => ({
          time: p.time as Time,
          value: p.value,
        })),
      );
    if (ma20Ref.current)
      ma20Ref.current.setData(
        calcMA(ohlcvInputs, 20).map((p) => ({
          time: p.time as Time,
          value: p.value,
        })),
      );
    if (ma60Ref.current)
      ma60Ref.current.setData(
        calcMA(ohlcvInputs, 60).map((p) => ({
          time: p.time as Time,
          value: p.value,
        })),
      );
    if (ma120Ref.current)
      ma120Ref.current.setData(
        calcMA(ohlcvInputs, 120).map((p) => ({
          time: p.time as Time,
          value: p.value,
        })),
      );

    if (
      bbUpperRef.current &&
      bbMiddleRef.current &&
      bbLowerRef.current
    ) {
      const bb = calcBollingerBands(ohlcvInputs);
      bbUpperRef.current.setData(
        bb.map((p) => ({ time: p.time as Time, value: p.upper })),
      );
      bbMiddleRef.current.setData(
        bb.map((p) => ({ time: p.time as Time, value: p.middle })),
      );
      bbLowerRef.current.setData(
        bb.map((p) => ({ time: p.time as Time, value: p.lower })),
      );
    }

    if (rsiSeriesRef.current) {
      const rsi = calcRsi(ohlcvInputs);
      rsiSeriesRef.current.setData(
        rsi.map((p) => ({ time: p.time as Time, value: p.value })),
      );
    }

    if (
      macdLineRef.current &&
      macdSignalRef.current &&
      macdHistRef.current
    ) {
      const macd = calcMacd(ohlcvInputs);
      macdLineRef.current.setData(
        macd.map((p) => ({ time: p.time as Time, value: p.macd })),
      );
      macdSignalRef.current.setData(
        macd.map((p) => ({ time: p.time as Time, value: p.signal })),
      );
      macdHistRef.current.setData(
        macd.map((p) => ({
          time: p.time as Time,
          value: p.histogram,
          color: p.histogram >= 0 ? theme?.up : theme?.down,
        })),
      );
    }

    chart.timeScale().fitContent();
  }, [bars, ohlcvInputs, active, interval]);

  useEffect(() => {
    const data = flowQuery.data;
    if (!data || !flowForeignRef.current || !flowInstRef.current || !flowIndivRef.current) return;

    flowForeignRef.current.setData(
      data.map((p: InvestorFlowPoint) => ({ time: p.time as Time, value: p.foreign }))
    );
    flowInstRef.current.setData(
      data.map((p: InvestorFlowPoint) => ({ time: p.time as Time, value: p.institution }))
    );
    flowIndivRef.current.setData(
      data.map((p: InvestorFlowPoint) => ({ time: p.time as Time, value: p.individual }))
    );
  }, [flowQuery.data]);

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
      const startedAt =
        time != null
          ? new Date(time * 1000).toISOString().replace("T", " ").slice(0, 19)
          : "-";
      const index = bars.findIndex(
        (b) => toChartTime(b.startedAt) === param.time,
      );
      const volume = index >= 0 ? bars[index].volume : 0;

      function getLineValue(ref: RefObject<ISeriesApi<"Line"> | null>): number | undefined {
        if (!ref.current) return undefined;
        const d = param.seriesData.get(ref.current);
        return d && "value" in d ? (d.value as number) : undefined;
      }
      function getHistValue(ref: RefObject<ISeriesApi<"Histogram"> | null>): number | undefined {
        if (!ref.current) return undefined;
        const d = param.seriesData.get(ref.current);
        return d && "value" in d ? (d.value as number) : undefined;
      }

      const indicators: IndicatorSnapshot = {
        ma5: getLineValue(ma5Ref),
        ma20: getLineValue(ma20Ref),
        ma60: getLineValue(ma60Ref),
        ma120: getLineValue(ma120Ref),
        bbUpper: getLineValue(bbUpperRef),
        bbMiddle: getLineValue(bbMiddleRef),
        bbLower: getLineValue(bbLowerRef),
        rsi: getLineValue(rsiSeriesRef),
        macd: getLineValue(macdLineRef),
        macdSignal: getLineValue(macdSignalRef),
        macdHist: getHistValue(macdHistRef),
      };

      onCrosshairChange({
        startedAt,
        open: point.open,
        high: point.high,
        low: point.low,
        close: point.close,
        volume,
        indicators,
      });
    };

    const onClick = (param: MouseEventParams<Time>) => {
      if (drawTool === "none" || !param.point) return;
      const logical = chart.timeScale().coordinateToLogical(param.point.x);
      const price = series.coordinateToPrice(param.point.y);
      if (logical == null || price == null) return;

      if (drawTool === "hline") {
        addHLine(chartKey, { id: crypto.randomUUID(), price });
        return;
      }

      if (drawTool === "cross") {
        addCrossLine(chartKey, {
          id: crypto.randomUUID(),
          logical: Number(logical),
          price,
        });
        return;
      }

      if (!pendingTrendStart) {
        setPendingTrendStart({ logical: Number(logical), price });
        return;
      }

      addTrendLine(chartKey, {
        id: crypto.randomUUID(),
        p1: pendingTrendStart,
        p2: { logical: Number(logical), price },
      });
      setPendingTrendStart(null);
    };

    const onRangeChanged = () => {
      forceUpdate((n) => n + 1);
    };

    chart.subscribeCrosshairMove(onMove);
    chart.subscribeClick(onClick);
    chart.timeScale().subscribeVisibleLogicalRangeChange(onRangeChanged);

    return () => {
      chart.unsubscribeCrosshairMove(onMove);
      chart.unsubscribeClick(onClick);
      chart.timeScale().unsubscribeVisibleLogicalRangeChange(onRangeChanged);
    };
  }, [
    bars,
    drawTool,
    pendingTrendStart,
    onCrosshairChange,
    chartKey,
    addTrendLine,
    addHLine,
    addCrossLine,
  ]);

  useEffect(() => {
    if (drawTool !== "trend") {
      setPendingTrendStart(null);
    }
  }, [drawTool]);

  return (
    <div className="relative w-full" style={{ height: chartHeight }}>
      <div className="w-full" style={{ height: chartHeight }} ref={rootRef} />
      <svg
        className="absolute inset-0 w-full pointer-events-none"
        style={{ height: chartHeight }}
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        {overlay.hSegments.map((line) => (
          <line
            key={line.id}
            x1="0"
            y1={(line.y / chartHeight) * 100}
            x2="100"
            y2={(line.y / chartHeight) * 100}
            className="stroke-chart-cross [stroke-dasharray:6_4] [stroke-width:1.4] [vector-effect:non-scaling-stroke]"
          />
        ))}
        {overlay.trendSegments.map((line) => (
          <line
            key={line.id}
            x1={
              (line.x1 / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100
            }
            y1={(line.y1 / chartHeight) * 100}
            x2={
              (line.x2 / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100
            }
            y2={(line.y2 / chartHeight) * 100}
            className="stroke-chart-rsi [stroke-width:1.8] [vector-effect:non-scaling-stroke]"
          />
        ))}
        {overlay.crossSegments.map((line) => (
          <g key={line.id}>
            <line
              x1="0"
              y1={(line.y / chartHeight) * 100}
              x2="100"
              y2={(line.y / chartHeight) * 100}
              className="stroke-chart-up [stroke-dasharray:4_3] [stroke-width:1.2] [vector-effect:non-scaling-stroke]"
            />
            <line
              x1={(line.x / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100}
              y1="0"
              x2={(line.x / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100}
              y2="100"
              className="stroke-chart-up [stroke-dasharray:4_3] [stroke-width:1.2] [vector-effect:non-scaling-stroke]"
            />
            <circle
              cx={(line.x / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100}
              cy={(line.y / chartHeight) * 100}
              r="0.6"
              className="fill-chart-up [vector-effect:non-scaling-stroke]"
            />
          </g>
        ))}
      </svg>
      {(trendLines.length > 0 || hLines.length > 0 || crossLines.length > 0) && (
        <Button
          type="button"
          variant="destructive"
          className="absolute right-2 top-2"
          onClick={() => {
            clearLines(chartKey);
            setPendingTrendStart(null);
          }}
        >
          선 전체 삭제
        </Button>
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
  const [crosshairInfo, setCrosshairInfo] = useState<CrosshairInfo | null>(
    null,
  );

  const chartKey = `${source}-${selectedSymbol}-${interval}`;

  const symbolsQuery = useQuery({
    queryKey: ["market-symbols", source],
    queryFn: () => fetchMarketSymbols(source, 300),
  });

  const filteredSymbols = useMemo(() => {
    const list = symbolsQuery.data ?? [];
    const q = keyword.trim().toLowerCase();
    if (!q) return list.slice(0, 80);
    return list
      .filter(
        (item) =>
          item.symbol.toLowerCase().includes(q) ||
          item.name.toLowerCase().includes(q),
      )
      .slice(0, 80);
  }, [symbolsQuery.data, keyword]);

  const barsQuery = useQuery({
    queryKey: [
      "market-unified-bars",
      source,
      selectedSymbol,
      interval,
      minuteLimit,
    ],
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
    <section className="flex flex-col gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <h2 className="text-[28px] font-bold tracking-tight">통합 시세 차트</h2>
        <p className="max-w-3xl text-sm text-muted-foreground sm:text-base">
          종목 선택 후 주봉/일봉/1·5·10분봉을 한 화면에서 전환해서 확인합니다.
        </p>
      </div>

      <div className="grid grid-cols-[minmax(18rem,22.5rem)_1fr] gap-4 max-lg:grid-cols-1">
        {/* Left column */}
        <div className="flex flex-col gap-3">
          <Card>
            <CardContent>
            <div className="grid grid-cols-[1fr_auto_auto] gap-2 max-lg:grid-cols-1">
              <Input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="종목 검색 (예: AAPL, 005930)"
                aria-label="종목 검색"
              />
              <NativeSelect
                aria-label="시세 데이터 소스"
                value={source}
                onChange={(e) => setSource(e.target.value as MarketSource)}
              >
                <NativeSelectOption value="yfinance">yfinance</NativeSelectOption>
                <NativeSelectOption value="pykrx">pykrx</NativeSelectOption>
              </NativeSelect>
              <Button variant="outline"
                onClick={() => symbolsQuery.refetch()}
              >
                새로고침
              </Button>
            </div>
            </CardContent>
          </Card>

          <Card className="gap-0">
            <CardHeader className="border-b"><CardTitle>종목 테이블</CardTitle></CardHeader>
            <CardContent className="max-h-90 overflow-auto px-0">
              <Table>
                <TableHeader className="sticky top-0 bg-muted"><TableRow><TableHead>심볼</TableHead><TableHead>이름</TableHead></TableRow></TableHeader>
                <TableBody>
                  {symbolsQuery.isLoading && <TableRow><TableCell colSpan={2}><Skeleton className="h-16 w-full" aria-label="종목 목록 로딩 중" /></TableCell></TableRow>}
                  {symbolsQuery.isError && <TableRow><TableCell colSpan={2} className="text-center text-destructive">종목 목록을 불러오지 못했습니다.</TableCell></TableRow>}
                  {!symbolsQuery.isLoading && !symbolsQuery.isError && filteredSymbols.length === 0 && <TableRow><TableCell colSpan={2} className="text-center text-muted-foreground">검색 결과가 없습니다.</TableCell></TableRow>}
                  {filteredSymbols.map((item) => (
                    <TableRow
                      key={`${item.symbol}-${item.name}`}
                      className={selectedSymbol === item.symbol ? "cursor-pointer bg-accent" : "cursor-pointer"}
                      onClick={() => setSelectedSymbol(item.symbol)}
                    >
                      <TableCell className={selectedSymbol === item.symbol ? "font-semibold text-primary" : "text-muted-foreground"}>{item.symbol}</TableCell>
                      <TableCell className={selectedSymbol === item.symbol ? "font-semibold text-primary" : "text-muted-foreground"}>{item.name}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>

        {/* Right column */}
        <div className="flex flex-col gap-3">
          <Card>
            <CardContent>
            {/* Interval tabs */}
            <div className="flex flex-wrap gap-2">
              {(["1w", "1d", "1m", "5m", "10m"] as UnifiedInterval[]).map(
                (v) => (
                  <Button
                    key={v}
                    variant={interval === v ? "secondary" : "outline"}
                    onClick={() => setInterval(v)}
                    type="button"
                  >
                    {INTERVAL_LABEL[v]}
                  </Button>
                ),
              )}
              {(interval === "1m" ||
                interval === "5m" ||
                interval === "10m") && (
                <NativeSelect
                  aria-label="분봉 조회 개수"
                  value={minuteLimit}
                  onChange={(e) => setMinuteLimit(Number(e.target.value))}
                >
                  <NativeSelectOption value={50}>50개</NativeSelectOption>
                  <NativeSelectOption value={100}>100개</NativeSelectOption>
                </NativeSelect>
              )}
            </div>

            {/* Draw tools */}
            <div className="flex flex-wrap gap-2 mt-2">
              <Button
                type="button"
                variant={drawTool === "none" ? "default" : "outline"}
                onClick={() => setDrawTool("none")}
              >
                선택
              </Button>
              <Button
                type="button"
                variant={drawTool === "trend" ? "default" : "outline"}
                onClick={() => setDrawTool("trend")}
              >
                추세선
              </Button>
              <Button
                type="button"
                variant={drawTool === "hline" ? "default" : "outline"}
                onClick={() => setDrawTool("hline")}
              >
                수평선
              </Button>
              <Button
                type="button"
                variant={drawTool === "cross" ? "default" : "outline"}
                onClick={() => setDrawTool("cross")}
              >
                크로스
              </Button>
            </div>

            {/* Indicator toggles */}
            <div className="flex flex-wrap gap-2 mt-2">
              <IndicatorToggleBar interval={interval} source={source} />
            </div>

            {/* Meta info */}
            <div className="flex gap-2 flex-wrap mt-2">
              <Badge variant="secondary">
                선택 종목: {selectedSymbol || "-"}
              </Badge>
              <Badge variant="secondary">
                주기: {INTERVAL_LABEL[interval]}
              </Badge>
              <Badge variant="secondary">
                데이터: {bars.length}건
              </Badge>
              {last && (
                <Badge variant="secondary">
                  최신 종가: {formatNumber(last.close)}
                </Badge>
              )}
              {crosshairInfo && (
                <>
                  <Badge variant="outline">
                    포인트: {crosshairInfo.startedAt}
                  </Badge>
                  <Badge variant="outline">
                    O: {formatNumber(crosshairInfo.open)}
                  </Badge>
                  <Badge variant="outline">
                    H: {formatNumber(crosshairInfo.high)}
                  </Badge>
                  <Badge variant="outline">
                    L: {formatNumber(crosshairInfo.low)}
                  </Badge>
                  <Badge variant="outline">
                    C: {formatNumber(crosshairInfo.close)}
                  </Badge>
                  <Badge variant="outline">
                    V: {formatNumber(crosshairInfo.volume)}
                  </Badge>
                  {crosshairInfo.indicators.ma5 !== undefined && (
                    <Badge variant="outline" className="text-chart-ma5">
                      MA5: {formatNumber(crosshairInfo.indicators.ma5)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.ma20 !== undefined && (
                    <Badge variant="outline" className="text-chart-ma20">
                      MA20: {formatNumber(crosshairInfo.indicators.ma20)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.ma60 !== undefined && (
                    <Badge variant="outline" className="text-chart-ma60">
                      MA60: {formatNumber(crosshairInfo.indicators.ma60)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.ma120 !== undefined && (
                    <Badge variant="outline" className="text-chart-ma120">
                      MA120: {formatNumber(crosshairInfo.indicators.ma120)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.bbUpper !== undefined && (
                    <Badge variant="outline" className="text-chart-band">
                      BB상단: {formatNumber(crosshairInfo.indicators.bbUpper)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.bbMiddle !== undefined && (
                    <Badge variant="outline" className="text-chart-band">
                      BB중단: {formatNumber(crosshairInfo.indicators.bbMiddle)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.bbLower !== undefined && (
                    <Badge variant="outline" className="text-chart-band">
                      BB하단: {formatNumber(crosshairInfo.indicators.bbLower)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.rsi !== undefined && (
                    <Badge variant="outline" className="text-chart-rsi">
                      RSI: {formatNumber(crosshairInfo.indicators.rsi)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.macd !== undefined && (
                    <Badge variant="outline" className="text-chart-ma20">
                      MACD: {formatNumber(crosshairInfo.indicators.macd)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.macdSignal !== undefined && (
                    <Badge variant="outline" className="text-chart-ma60">
                      Signal: {formatNumber(crosshairInfo.indicators.macdSignal)}
                    </Badge>
                  )}
                  {crosshairInfo.indicators.macdHist !== undefined && (
                    <Badge variant="outline" className={crosshairInfo.indicators.macdHist >= 0 ? "text-chart-up" : "text-chart-down"}>
                      HIST: {formatNumber(crosshairInfo.indicators.macdHist)}
                    </Badge>
                  )}
                </>
              )}
            </div>
            </CardContent>
          </Card>

          {/* Chart area */}
          <Card className="min-h-95 w-full">
            <CardContent>
            {barsQuery.isLoading && <Skeleton className="h-95 w-full" aria-label="차트 데이터 로딩 중" />}
            {barsQuery.isError && <Alert variant="destructive"><AlertDescription>차트 조회 중 오류가 발생했습니다.</AlertDescription></Alert>}
            {!barsQuery.isLoading &&
              !barsQuery.isError &&
              bars.length === 0 && <Alert><AlertDescription>데이터가 없습니다.</AlertDescription></Alert>}
            {!barsQuery.isLoading && !barsQuery.isError && bars.length > 0 && (
              <UnifiedCandlestickChart
                bars={bars}
                drawTool={drawTool}
                chartKey={chartKey}
                interval={interval}
                source={source}
                symbol={selectedSymbol}
                onCrosshairChange={setCrosshairInfo}
              />
            )}
            </CardContent>
          </Card>

          {/* OHLCV table */}
          <Card className="gap-0">
            <CardHeader className="border-b"><CardTitle>OHLCV 테이블</CardTitle></CardHeader>
            <CardContent className="min-h-75 overflow-x-auto px-0">
              <Table>
                <TableHeader><TableRow><TableHead>시각/일자</TableHead><TableHead>시가</TableHead><TableHead>고가</TableHead><TableHead>저가</TableHead><TableHead>종가</TableHead><TableHead>거래량</TableHead></TableRow></TableHeader>
                <TableBody>
                  {bars
                    .slice()
                    .reverse()
                    .map((bar) => (
                      <TableRow
                        key={`${bar.startedAt}-${bar.close}`}
                      >
                        <TableCell className="text-muted-foreground">{bar.startedAt}</TableCell>
                        <TableCell className="tabular-nums text-muted-foreground">{formatNumber(bar.open)}</TableCell>
                        <TableCell className="tabular-nums text-muted-foreground">{formatNumber(bar.high)}</TableCell>
                        <TableCell className="tabular-nums text-muted-foreground">{formatNumber(bar.low)}</TableCell>
                        <TableCell className="tabular-nums text-muted-foreground">{formatNumber(bar.close)}</TableCell>
                        <TableCell className="tabular-nums text-muted-foreground">{formatNumber(bar.volume)}</TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      </div>
    </section>
  );
}
