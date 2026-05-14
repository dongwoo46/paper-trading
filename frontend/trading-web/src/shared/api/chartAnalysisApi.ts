export type AnalysisSummary = { recommendation: string; confidence: string };
export type Levels = {
  supports: string[];
  resistances: string[];
  entry: string;
  stop_loss: string;
  target: string;
  risk_reward: string;
};
export type Trend = {
  direction: string;
  strength: string;
  ma_alignment: string;
  adx: string;
  hh_ll_structure: string;
};
export type Pattern = { type: string; index: number; date: string };
export type IndicatorSignal = { name: string; value: string; interpretation: string };
export type Technical = { trend: Trend; patterns: Pattern[]; indicator_signals: IndicatorSignal[] };
export type Volume = { trend: string; spike_detected: boolean; avg_ratio: string };
export type ReportSlot = { status: string; narrative: LlmNarrative | null };
export type ReportStatus = { llm: ReportSlot; template: ReportSlot };
export type AnalysisWindow = {
  window: string;
  interval: string;
  computed_at: string;
  snapshot_hash: string;
  summary: AnalysisSummary;
  levels: Levels;
  technical: Technical;
  volume: Volume;
  report: ReportStatus;
};
export type ChartAnalysisResponse = { symbol: string; analyses: AnalysisWindow[] };

export type LlmNarrative = {
  trend_section: string;
  support_resistance_section: string;
  entry_plan_section: string;
  signal_evidence_section: string;
  risk_section: string;
};

export type LlmReportData = {
  window: string;
  interval: string;
  source: string;
  narrative: LlmNarrative;
};

const QUANT_API = "/api-ai";
const RESEARCH_API = "/api-research";

export async function fetchChartAnalysis(symbol: string): Promise<ChartAnalysisResponse> {
  const res = await fetch(`${RESEARCH_API}/research/results/${encodeURIComponent(symbol)}`);
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(`${res.status}: ${msg}`);
  }
  return res.json() as Promise<ChartAnalysisResponse>;
}

const COLLECTOR_API = "/api-collector";

export type CollectSymbolRequest = {
  symbol: string;
  provider: "yfinance" | "pykrx";
  start: string;
  end: string;
};

export type CollectSymbolResponse = {
  symbol: string;
  provider: string;
  rows_inserted: number;
  start: string;
  end: string;
  success: boolean;
  error: string | null;
};

export type RunAnalysisResponse = {
  symbol: string;
  success: number;
  failed: number;
  skipped: number;
};

export type CollectBatchRequest = {
  provider: "all" | "yfinance" | "pykrx" | "kis";
  start?: string;
  end?: string;
  only_default?: boolean;
  auto_adjust?: boolean;
  adjusted?: boolean;
};

export type CollectBatchResponse = {
  provider: string;
  symbols: number;
  success_symbols: number;
  failed_symbols: number;
  skipped_symbols?: number;
  total_rows_inserted: number;
  start: string;
  end: string;
};

export async function collectSymbolDaily(req: CollectSymbolRequest): Promise<CollectSymbolResponse> {
  const res = await fetch(`${COLLECTOR_API}/collect/symbol/daily`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json() as Promise<CollectSymbolResponse>;
}

export async function collectSymbolWeekly(req: CollectSymbolRequest): Promise<CollectSymbolResponse> {
  const res = await fetch(`${COLLECTOR_API}/collect/symbol/weekly`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json() as Promise<CollectSymbolResponse>;
}

export async function collectBatchDaily(req: CollectBatchRequest): Promise<CollectBatchResponse> {
  const res = await fetch(`${COLLECTOR_API}/collect/daily`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json() as Promise<CollectBatchResponse>;
}

export async function collectBatchWeekly(req: CollectBatchRequest): Promise<CollectBatchResponse> {
  const res = await fetch(`${COLLECTOR_API}/collect/weekly`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json() as Promise<CollectBatchResponse>;
}

export async function runChartAnalysis(symbol: string): Promise<RunAnalysisResponse> {
  const res = await fetch(`${RESEARCH_API}/research/run/${encodeURIComponent(symbol)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
  });
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json() as Promise<RunAnalysisResponse>;
}

export type AnalyzedSymbolItem = {
  symbol: string;
  windows: number;
  last_analyzed: string | null;
};

export async function fetchAnalyzedSymbols(): Promise<AnalyzedSymbolItem[]> {
  const res = await fetch(`${RESEARCH_API}/research/symbols`);
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  const data = (await res.json()) as { symbols: AnalyzedSymbolItem[] };
  return data.symbols;
}

export type CatalogSymbolItem = {
  symbol: string;
  name: string;
  market: string;
  provider: "yfinance" | "pykrx";
};

export type CollectedSymbolItem = {
  source: "yfinance" | "pykrx" | "kis";
  symbol: string;
  name: string;
  market: string;
  daily_bars: number;
  weekly_bars: number;
  last_daily_date: string | null;
  last_weekly_date: string | null;
};

export async function fetchCatalogSymbols(): Promise<CatalogSymbolItem[]> {
  const res = await fetch(`${COLLECTOR_API}/symbols`);
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  const data = (await res.json()) as { symbols: CatalogSymbolItem[] };
  return data.symbols;
}

export async function fetchCollectedSymbols(source: "all" | "yfinance" | "pykrx" | "kis" = "all"): Promise<CollectedSymbolItem[]> {
  const params = new URLSearchParams({ source });
  const res = await fetch(`${COLLECTOR_API}/market/collected-symbols?${params.toString()}`);
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json() as Promise<CollectedSymbolItem[]>;
}

export type TriggerLlmReportResponse = {
  status: "queued" | "pending" | "already_done" | "not_found" | "not_popular";
};

export async function triggerLlmReport(
  symbol: string,
  window: string,
  interval: string,
): Promise<TriggerLlmReportResponse> {
  const params = new URLSearchParams({ window, interval });
  const res = await fetch(
    `${QUANT_API}/chart-analysis/${encodeURIComponent(symbol)}/report?${params.toString()}`,
    { method: "POST", headers: { "Content-Type": "application/json" } },
  );
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json() as Promise<TriggerLlmReportResponse>;
}

export type LlmDoneEvent = {
  type: "llm_done";
  symbol: string;
  name: string;
  window: string;
  interval: string;
  source: string;
};

export function subscribeAnalysisNotifications(
  onDone: (event: LlmDoneEvent) => void,
): () => void {
  const es = new EventSource(`${QUANT_API}/chart-analysis/notifications`);

  es.addEventListener("llm_done", (e: MessageEvent) => {
    try {
      const data = JSON.parse(e.data as string) as LlmDoneEvent;
      onDone(data);
    } catch {
      // 파싱 실패는 무시
    }
  });

  return () => es.close();
}
