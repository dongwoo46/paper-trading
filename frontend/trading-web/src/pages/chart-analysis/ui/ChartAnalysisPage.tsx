import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BarChart3, Database, Info, RefreshCw, Search, Sparkles, X } from "lucide-react";
import {
  collectBatchDaily,
  collectBatchWeekly,
  fetchChartAnalysis,
  fetchCollectedSymbols,
  runChartAnalysis,
  triggerLlmReport,
  type AnalysisWindow,
  type ChartAnalysisResponse,
  type CollectBatchResponse,
  type CollectedSymbolItem,
  type IndicatorSignal,
  type LlmNarrative,
  type Pattern,
  type RunAnalysisResponse,
  type TriggerLlmReportResponse,
} from "../../../shared/api/chartAnalysisApi";

const WINDOWS = ["1M", "3M", "6M", "1Y", "2Y", "MAX"];

type CollectionCommand = {
  id: "yfinance-daily" | "kis-daily" | "kis-weekly" | "yfinance-weekly";
  label: string;
  provider: "yfinance" | "kis";
  interval: "daily" | "weekly";
};

const COLLECTION_COMMANDS: CollectionCommand[] = [
  { id: "yfinance-daily", label: "yfinance 전체 일봉 수집", provider: "yfinance", interval: "daily" },
  { id: "kis-daily", label: "KIS 전체 일봉 수집", provider: "kis", interval: "daily" },
  { id: "kis-weekly", label: "KIS 전체 주봉 수집", provider: "kis", interval: "weekly" },
  { id: "yfinance-weekly", label: "yfinance 전체 주봉 수집", provider: "yfinance", interval: "weekly" },
];

const panelClass = "bg-bg-card border border-border-primary rounded-lg shadow-sm";
const buttonBase = "inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition-all disabled:opacity-50 disabled:cursor-not-allowed";
const HELP_TEXT: Record<string, { title: string; body: string }> = {
  summary: {
    title: "추천 요약",
    body: "분석 결과의 최종 판단입니다. 추세, 패턴, 지표 신호, 거래량 분석을 점수화해 BUY/SELL/HOLD와 신뢰도를 보여줍니다.",
  },
  trend: {
    title: "추세",
    body: "이동평균 배열, ADX, 고점/저점 구조를 이용해 현재 가격 흐름이 상승/하락/횡보인지와 강도를 보여줍니다.",
  },
  levels: {
    title: "가격 레벨",
    body: "최근 캔들 고저점과 ATR 변동성을 바탕으로 지지선, 저항선, 진입가, 손절가, 목표가, 손익비를 계산합니다.",
  },
  volume: {
    title: "거래량/패턴",
    body: "거래량은 가격 움직임에 얼마나 많은 사람이 참여했는지를 보여줍니다. 평균 대비 거래량이 크면 현재 가격 움직임의 신뢰도가 높아질 수 있고, 캔들 패턴은 매수·매도 심리의 흔적을 의미합니다.",
  },
  indicators: {
    title: "지표 신호",
    body: "RSI, MACD, 볼린저밴드, ATR, ADX, 스토캐스틱, OBV, 거래량 이동평균을 초보자가 읽기 쉬운 신호로 변환해 보여줍니다.",
  },
  llm: {
    title: "LLM 설명",
    body: "저장된 수치 분석 결과를 바탕으로 추세, 지지/저항, 진입 계획, 근거, 리스크를 자연어 문서로 설명합니다. 생성된 설명은 DB에 저장됩니다.",
  },
};

const WINDOW_LABEL: Record<string, string> = {
  "1M": "1개월",
  "3M": "3개월",
  "6M": "6개월",
  "1Y": "1년",
  "2Y": "2년",
  MAX: "전체",
};

function recommendationClass(rec: string): string {
  const upper = rec.toUpperCase();
  if (upper === "BUY") return "bg-red-50 text-red-600 border-red-100";
  if (upper === "SELL") return "bg-blue-50 text-blue-600 border-blue-100";
  return "bg-amber-50 text-amber-700 border-amber-100";
}

function directionClass(dir: string): string {
  const upper = dir.toUpperCase();
  if (upper === "UP" || upper === "BULLISH") return "bg-red-50 text-red-600 border-red-100";
  if (upper === "DOWN" || upper === "BEARISH") return "bg-blue-50 text-blue-600 border-blue-100";
  return "bg-gray-100 text-text-secondary border-border-primary";
}

function Badge({ children, className = "" }: { children: React.ReactNode; className?: string }) {
  return <span className={`inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-semibold ${className}`}>{children}</span>;
}

function explainVolumeTrend(trend: string): string {
  if (trend === "increasing") return "최근 거래량이 평소보다 늘었습니다. 가격 움직임에 참여자가 늘었다는 뜻입니다.";
  if (trend === "decreasing") return "최근 거래량이 평소보다 줄었습니다. 가격 움직임의 힘이 약할 수 있습니다.";
  return "최근 거래량이 평균과 비슷합니다. 특별한 거래량 변화는 크지 않습니다.";
}

function explainVolumeRatio(ratio: string): string {
  const value = Number(ratio);
  if (!Number.isFinite(value)) return "평균 거래량과 비교한 비율입니다.";
  if (value >= 2) return "평균의 2배 이상 거래됐습니다. 시장 관심이 크게 몰린 구간입니다.";
  if (value >= 1.2) return "평균보다 거래가 많은 편입니다. 움직임에 힘이 실렸는지 확인할 만합니다.";
  if (value <= 0.8) return "평균보다 거래가 적습니다. 움직임의 신뢰도가 약할 수 있습니다.";
  return "평균과 비슷한 거래량입니다.";
}

function explainPattern(patterns: Pattern[]): string {
  if (patterns.length === 0) return "특별한 캔들 패턴이 감지되지 않았습니다. 패턴보다는 추세와 지표를 더 봐야 합니다.";
  return "감지된 패턴은 특정 날짜의 매수·매도 심리 흔적입니다. 단독 매매 신호가 아니라 추세·거래량과 함께 봐야 합니다.";
}


function HelpButton({ topic, onInfo }: { topic: keyof typeof HELP_TEXT; onInfo: (topic: keyof typeof HELP_TEXT) => void }) {
  return (
    <button
      type="button"
      onClick={() => onInfo(topic)}
      className="inline-flex h-6 w-6 items-center justify-center rounded-full border border-border-primary bg-bg-input text-text-muted hover:border-brand-primary hover:text-brand-primary"
      aria-label={`${HELP_TEXT[topic].title} 설명`}
    >
      <Info size={13} />
    </button>
  );
}

function HelpModal({ topic, onClose }: { topic: keyof typeof HELP_TEXT | null; onClose: () => void }) {
  if (!topic) return null;
  const help = HELP_TEXT[topic];
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 px-4">
      <div className="w-full max-w-md rounded-xl border border-border-primary bg-bg-card p-5 shadow-xl">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h3 className="text-lg font-bold text-text-primary">{help.title}</h3>
          <button type="button" onClick={onClose} className="rounded-full p-1 text-text-muted hover:bg-bg-input hover:text-text-primary">
            <X size={18} />
          </button>
        </div>
        <p className="text-sm leading-6 text-text-secondary">{help.body}</p>
      </div>
    </div>
  );
}

function SummaryCard({ win, onInfo }: { win: AnalysisWindow; onInfo: (topic: keyof typeof HELP_TEXT) => void }) {
  return (
    <section className={`${panelClass} p-5`}>
      <div className="flex flex-wrap items-center gap-4">
        <HelpButton topic="summary" onInfo={onInfo} />
        <div className="flex items-center gap-2">
          <span className="text-sm text-text-muted">추천</span>
          <Badge className={recommendationClass(win.summary.recommendation)}>{win.summary.recommendation}</Badge>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-text-muted">신뢰도</span>
          <span className="text-sm font-semibold text-text-primary">{win.summary.confidence}</span>
        </div>
        <div className="ml-auto text-xs text-text-muted">분석 시각 {win.computed_at}</div>
      </div>
    </section>
  );
}

function TrendCard({ win, onInfo }: { win: AnalysisWindow; onInfo: (topic: keyof typeof HELP_TEXT) => void }) {
  const { trend } = win.technical;
  return (
    <section className={`${panelClass} p-5`}>
      <div className="mb-3 flex items-center gap-2"><h3 className="text-[15px] font-semibold text-text-primary">추세</h3><HelpButton topic="trend" onInfo={onInfo} /></div>
      <div className="mb-3 flex flex-wrap gap-2">
        <Badge className={directionClass(trend.direction)}>{trend.direction}</Badge>
        <Badge className={directionClass(trend.strength)}>{trend.strength}</Badge>
      </div>
      <dl className="grid gap-2 text-sm">
        <div className="flex justify-between gap-4"><dt className="text-text-muted">MA 정렬</dt><dd className="text-right text-text-primary">{trend.ma_alignment}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">ADX</dt><dd className="text-right text-text-primary">{trend.adx}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">HH/LL</dt><dd className="text-right text-text-primary">{trend.hh_ll_structure}</dd></div>
      </dl>
    </section>
  );
}

function LevelsCard({ win, onInfo }: { win: AnalysisWindow; onInfo: (topic: keyof typeof HELP_TEXT) => void }) {
  const { levels } = win;
  return (
    <section className={`${panelClass} p-5`}>
      <div className="mb-3 flex items-center gap-2"><h3 className="text-[15px] font-semibold text-text-primary">가격 레벨</h3><HelpButton topic="levels" onInfo={onInfo} /></div>
      <dl className="grid gap-2 text-sm">
        <div className="flex justify-between gap-4"><dt className="text-text-muted">지지</dt><dd className="text-right text-text-primary">{levels.supports.join(", ") || "-"}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">저항</dt><dd className="text-right text-text-primary">{levels.resistances.join(", ") || "-"}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">진입</dt><dd className="text-right font-semibold text-brand-primary">{levels.entry}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">손절</dt><dd className="text-right font-semibold text-blue-600">{levels.stop_loss}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">목표</dt><dd className="text-right font-semibold text-red-600">{levels.target}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">R/R</dt><dd className="text-right text-text-primary">{levels.risk_reward}</dd></div>
      </dl>
    </section>
  );
}

function VolumePatternCard({ win, onInfo }: { win: AnalysisWindow; onInfo: (topic: keyof typeof HELP_TEXT) => void }) {
  const patterns: Pattern[] = win.technical.patterns;
  return (
    <section className={`${panelClass} p-5`}>
      <div className="mb-3 flex items-center gap-2"><h3 className="text-[15px] font-semibold text-text-primary">거래량/패턴</h3><HelpButton topic="volume" onInfo={onInfo} /></div>
      <dl className="grid gap-2 text-sm">
        <div className="flex justify-between gap-4"><dt className="text-text-muted">거래량 추세</dt><dd className="text-right text-text-primary">{win.volume.trend}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">스파이크</dt><dd className="text-right text-text-primary">{win.volume.spike_detected ? "감지" : "없음"}</dd></div>
        <div className="flex justify-between gap-4"><dt className="text-text-muted">평균 비율</dt><dd className="text-right text-text-primary">{win.volume.avg_ratio}</dd></div>
      </dl>
      <div className="mt-4 rounded-lg bg-bg-input p-3 text-sm leading-6 text-text-secondary">
        <p>{explainVolumeTrend(win.volume.trend)}</p>
        <p>{explainVolumeRatio(win.volume.avg_ratio)}</p>
        <p>{win.volume.spike_detected ? "거래량 스파이크는 평소보다 거래가 갑자기 몰렸다는 뜻입니다." : "거래량 스파이크는 없습니다. 급격한 관심 집중 신호는 약합니다."}</p>
        <p>{explainPattern(patterns)}</p>
      </div>
      <div className="mt-4 flex flex-wrap gap-2">
        {patterns.length === 0 ? (
          <span className="text-sm text-text-muted">감지된 패턴 없음</span>
        ) : (
          patterns.map((p, i) => <Badge key={`${p.type}-${i}`} className="bg-bg-input text-text-secondary border-border-primary">{p.type} · {p.date}</Badge>)
        )}
      </div>
    </section>
  );
}

function WindowMetaCard({ win }: { win: AnalysisWindow }) {
  return (
    <section className={`${panelClass} p-5`}>
      <div className="grid gap-3 text-sm md:grid-cols-4">
        <div>
          <p className="text-xs font-semibold text-text-muted">윈도우</p>
          <p className="font-semibold text-text-primary">{WINDOW_LABEL[win.window] ?? win.window}</p>
        </div>
        <div>
          <p className="text-xs font-semibold text-text-muted">봉 종류</p>
          <p className="font-semibold text-text-primary">{win.interval === "D" ? "일봉" : "주봉"}</p>
        </div>
        <div>
          <p className="text-xs font-semibold text-text-muted">리포트 상태</p>
          <p className="font-semibold text-text-primary">LLM: {win.report.llm.status} / 템플릿: {win.report.template.status}</p>
        </div>
        <div>
          <p className="text-xs font-semibold text-text-muted">스냅샷</p>
          <p className="break-all font-mono text-xs text-text-secondary">{win.snapshot_hash}</p>
        </div>
      </div>
    </section>
  );
}

function PatternDetailCard({ patterns }: { patterns: Pattern[] }) {
  return (
    <section className={`${panelClass} overflow-hidden`}>
      <div className="border-b border-border-primary px-5 py-4">
        <h3 className="text-[15px] font-semibold text-text-primary">패턴 상세</h3>
      </div>
      {patterns.length === 0 ? (
        <p className="px-5 py-4 text-sm text-text-muted">감지된 캔들 패턴이 없습니다.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-bg-input text-text-muted">
              <tr>
                <th className="px-5 py-3 text-left font-semibold">패턴</th>
                <th className="px-5 py-3 text-left font-semibold">날짜</th>
                <th className="px-5 py-3 text-right font-semibold">봉 위치</th>
              </tr>
            </thead>
            <tbody>
              {patterns.map((p, i) => (
                <tr key={`${p.type}-${p.date}-${i}`} className="border-t border-border-primary">
                  <td className="px-5 py-3 font-medium text-text-primary">{p.type}</td>
                  <td className="px-5 py-3 text-text-secondary">{p.date}</td>
                  <td className="px-5 py-3 text-right text-text-secondary">{p.index}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function NarrativeSections({ narrative }: { narrative: LlmNarrative }) {
  return (
    <div className="grid gap-3 md:grid-cols-2">
      {(
        [
          ["추세", narrative.trend_section],
          ["지지/저항", narrative.support_resistance_section],
          ["진입 계획", narrative.entry_plan_section],
          ["근거", narrative.signal_evidence_section],
          ["리스크", narrative.risk_section],
        ] as [string, string][]
      ).map(([label, content]) => (
        <div key={label} className="rounded-lg bg-bg-input p-4">
          <p className="mb-1 text-xs font-semibold text-text-muted">{label}</p>
          <p className="text-sm leading-6 text-text-secondary">{content ?? "-"}</p>
        </div>
      ))}
    </div>
  );
}

function ChartNarrativeCard({
  llmStatus,
  templateNarrative,
  llmNarrative,
  error,
  onInfo,
}: {
  llmStatus: string | null;
  templateNarrative: LlmNarrative | null;
  llmNarrative: LlmNarrative | null;
  error: string | null;
  onInfo: (topic: keyof typeof HELP_TEXT) => void;
}) {
  const isGenerating = llmStatus === "queued";

  return (
    <div className="flex flex-col gap-4">
      {/* 템플릿 섹션 — 항상 표시. 없으면 안내 메시지 */}
      <section className={`${panelClass} p-5`}>
        <div className="mb-4 flex items-center gap-2">
          <Sparkles size={17} className="text-text-secondary" />
          <h3 className="text-[15px] font-semibold text-text-primary">템플릿 분석</h3>
          <HelpButton topic="llm" onInfo={onInfo} />
          <Badge className="bg-bg-input text-text-secondary border-border-primary">룰 기반</Badge>
        </div>
        {templateNarrative ? (
          <NarrativeSections narrative={templateNarrative} />
        ) : (
          <p className="text-sm text-text-muted">
            템플릿 리포트가 없습니다.{" "}
            <span className="font-semibold text-text-primary">해당 종목 분석하기</span> 버튼을 눌러 생성하세요.
          </p>
        )}
      </section>

      {/* LLM 섹션 — 항상 표시. 없으면 안내 메시지 */}
      <section className={`${panelClass} p-5`}>
        <div className="mb-4 flex items-center gap-2">
          <Sparkles size={17} className="text-brand-primary" />
          <h3 className="text-[15px] font-semibold text-text-primary">LLM 분석</h3>
          <HelpButton topic="llm" onInfo={onInfo} />
          <Badge className="bg-brand-secondary text-brand-primary border-brand-secondary">AI 생성</Badge>
          {isGenerating && <span className="animate-pulse text-xs text-brand-primary">백그라운드 생성 중...</span>}
        </div>
        {llmNarrative ? (
          <NarrativeSections narrative={llmNarrative} />
        ) : isGenerating ? (
          <p className="text-sm text-text-muted">LLM 리포트를 백그라운드에서 생성 중입니다. 완료되면 알림이 표시됩니다.</p>
        ) : (
          <p className="text-sm text-text-muted">
            {llmStatus && LLM_STATUS_LABEL[llmStatus]
              ? LLM_STATUS_LABEL[llmStatus]
              : <>LLM 리포트가 없습니다.{" "}<span className="font-semibold text-brand-primary">LLM 차트 분석 요청</span> 버튼을 눌러 생성하세요.</>}
          </p>
        )}
        {error && <p className="mt-2 text-sm text-blue-600">{error}</p>}
      </section>
    </div>
  );
}

function IndicatorTable({ signals, onInfo }: { signals: IndicatorSignal[]; onInfo: (topic: keyof typeof HELP_TEXT) => void }) {
  return (
    <section className={`${panelClass} overflow-hidden`}>
      <div className="border-b border-border-primary px-5 py-4">
        <div className="flex items-center gap-2"><h3 className="text-[15px] font-semibold text-text-primary">지표 신호</h3><HelpButton topic="indicators" onInfo={onInfo} /></div>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-bg-input text-text-muted">
            <tr>
              <th className="px-5 py-3 text-left font-semibold">지표</th>
              <th className="px-5 py-3 text-left font-semibold">값</th>
              <th className="px-5 py-3 text-left font-semibold">해석</th>
            </tr>
          </thead>
          <tbody>
            {signals.map((sig, i) => (
              <tr key={`${sig.name}-${i}`} className="border-t border-border-primary">
                <td className="px-5 py-3 font-medium text-text-primary">{sig.name}</td>
                <td className="px-5 py-3 text-text-secondary">{sig.value}</td>
                <td className="px-5 py-3 text-text-secondary">{sig.interpretation}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function CollectionResult({ result }: { result: CollectBatchResponse | null }) {
  if (!result) return <span className="text-sm text-text-muted">아직 실행된 수집 작업이 없습니다.</span>;
  return (
    <div className="flex flex-wrap gap-2 text-sm">
      <Badge className="bg-bg-input text-text-primary border-border-primary">대상 {result.symbols}</Badge>
      <Badge className="bg-red-50 text-red-600 border-red-100">성공 {result.success_symbols}</Badge>
      <Badge className="bg-blue-50 text-blue-600 border-blue-100">실패 {result.failed_symbols}</Badge>
      <Badge className="bg-amber-50 text-amber-700 border-amber-100">스킵 {result.skipped_symbols ?? 0}</Badge>
      <Badge className="bg-bg-input text-text-primary border-border-primary">저장 {result.total_rows_inserted}행</Badge>
      <Badge className="bg-bg-input text-text-secondary border-border-primary">{result.start} ~ {result.end}</Badge>
    </div>
  );
}

const LLM_STATUS_LABEL: Record<string, string> = {
  pending: "이미 처리 중인 요청이 있습니다. 잠시 후 다시 시도하세요.",
  already_done: "이미 LLM 리포트가 존재합니다.",
  not_found: "분석 결과가 없습니다. 먼저 해당 종목 분석하기를 실행하세요.",
  not_popular: "비인기 종목입니다.",
};

export function ChartAnalysisPage() {
  const queryClient = useQueryClient();
  const [selectedSymbol, setSelectedSymbol] = useState<CollectedSymbolItem | null>(null);
  const [symbolFilter, setSymbolFilter] = useState("");
  const [sourceFilter, setSourceFilter] = useState<"all" | "yfinance" | "pykrx" | "kis">("all");
  const [selectedWindow, setSelectedWindow] = useState(WINDOWS[0]);
  const [lastCollection, setLastCollection] = useState<CollectBatchResponse | null>(null);
  const [llmStatus, setLlmStatus] = useState<string | null>(null);
  const [helpTopic, setHelpTopic] = useState<keyof typeof HELP_TEXT | null>(null);

  const collectedQuery = useQuery<CollectedSymbolItem[]>({
    queryKey: ["collected-symbols", sourceFilter],
    queryFn: () => fetchCollectedSymbols(sourceFilter),
    staleTime: 30_000,
  });

  const filteredSymbols = useMemo(() => {
    const q = symbolFilter.trim().toLowerCase();
    return (collectedQuery.data ?? []).filter((item) => {
      if (!q) return true;
      return (
        item.symbol.toLowerCase().includes(q) ||
        item.name.toLowerCase().includes(q) ||
        item.market.toLowerCase().includes(q)
      );
    });
  }, [collectedQuery.data, symbolFilter]);

  const analysisQuery = useMutation<ChartAnalysisResponse, Error, string>({
    mutationFn: fetchChartAnalysis,
    onSuccess: (data) => {
      if (data.analyses.length > 0) setSelectedWindow(data.analyses[0].window);
      setLlmStatus(null);
    },
  });

  const collectMutation = useMutation<CollectBatchResponse, Error, CollectionCommand>({
    mutationFn: (cmd) => {
      const body = {
        provider: cmd.provider,
        only_default: false,
        auto_adjust: false,
        adjusted: false,
      };
      return cmd.interval === "daily" ? collectBatchDaily(body) : collectBatchWeekly(body);
    },
    onSuccess: (data) => {
      setLastCollection(data);
      void queryClient.invalidateQueries({ queryKey: ["collected-symbols"] });
    },
  });

  const runAnalysisMutation = useMutation<RunAnalysisResponse, Error, string>({
    mutationFn: runChartAnalysis,
    onSuccess: (_result, symbol) => {
      analysisQuery.mutate(symbol);
    },
  });

  const llmRequestMutation = useMutation<TriggerLlmReportResponse, Error, { symbol: string; window: string; interval: string }>({
    mutationFn: ({ symbol, window, interval }) => triggerLlmReport(symbol, window, interval),
    onSuccess: (data, { symbol }) => {
      setLlmStatus(data.status);
      if (data.status === "already_done") {
        analysisQuery.mutate(symbol);
      }
    },
  });

  const data = analysisQuery.data;
  const activeWin: AnalysisWindow | undefined = data?.analyses.find((a) => a.window === selectedWindow) ?? data?.analyses[0];
  const availableWindows = data ? WINDOWS.filter((w) => data.analyses.some((a) => a.window === w)) : WINDOWS;
  const selectedSymbolText = selectedSymbol?.symbol ?? "";

  return (
    <div className="flex flex-col gap-6">
      <HelpModal topic={helpTopic} onClose={() => setHelpTopic(null)} />

      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-[26px] font-bold text-text-primary">차트 분석</h2>
          <p className="text-sm text-text-secondary">OHLCV 수집, 종목 분석, LLM 설명 저장 상태를 한 화면에서 처리합니다.</p>
        </div>
        <button
          className={`${buttonBase} border border-border-primary bg-bg-card text-text-primary hover:bg-bg-input`}
          onClick={() => collectedQuery.refetch()}
        >
          <RefreshCw size={16} />
          수집 종목 새로고침
        </button>
      </header>

      <section className={`${panelClass} p-5`}>
        <div className="mb-4 flex items-center gap-2">
          <Database size={18} className="text-brand-primary" />
          <h3 className="text-[17px] font-semibold text-text-primary">전체 데이터 수집</h3>
        </div>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
          {COLLECTION_COMMANDS.map((cmd) => {
            const pending = collectMutation.isPending && collectMutation.variables?.id === cmd.id;
            return (
              <button
                key={cmd.id}
                className={`${buttonBase} min-h-[54px] border border-border-primary bg-bg-input text-text-primary hover:border-brand-primary hover:bg-brand-secondary`}
                onClick={() => collectMutation.mutate(cmd)}
                disabled={collectMutation.isPending}
              >
                {pending ? <RefreshCw size={16} className="animate-spin" /> : <Database size={16} />}
                {pending ? "수집 중..." : cmd.label}
              </button>
            );
          })}
        </div>
        <div className="mt-4 rounded-lg bg-bg-input p-4">
          <CollectionResult result={lastCollection} />
          {collectMutation.isError && <p className="mt-2 text-sm text-blue-600">{collectMutation.error.message}</p>}
        </div>
      </section>

      <section className={`${panelClass} overflow-hidden`}>
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border-primary px-5 py-4">
          <div className="flex items-center gap-2">
            <BarChart3 size={18} className="text-brand-primary" />
            <h3 className="text-[17px] font-semibold text-text-primary">수집된 종목</h3>
          </div>
          <div className="flex flex-wrap gap-2">
            <div className="relative">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
              <input
                value={symbolFilter}
                onChange={(e) => setSymbolFilter(e.target.value)}
                placeholder="종목/이름/시장 검색"
                className="h-10 w-[180px] rounded-lg border border-border-primary bg-bg-input pl-9 pr-3 text-sm outline-none focus:border-brand-primary"
              />
            </div>
            <select
              value={sourceFilter}
              onChange={(e) => setSourceFilter(e.target.value as "all" | "yfinance" | "pykrx" | "kis")}
              className="h-10 rounded-lg border border-border-primary bg-bg-input px-3 text-sm outline-none focus:border-brand-primary"
            >
              <option value="all">전체</option>
              <option value="yfinance">yfinance</option>
              <option value="kis">KIS</option>
              <option value="pykrx">pykrx</option>
            </select>
          </div>
        </div>
        <div className="max-h-[360px] overflow-auto">
          <table className="w-full text-sm">
            <thead className="sticky top-0 bg-bg-input text-text-muted">
              <tr>
                <th className="px-5 py-3 text-left font-semibold">종목</th>
                <th className="px-5 py-3 text-left font-semibold">종목명</th>
                <th className="px-5 py-3 text-left font-semibold">소스</th>
                <th className="px-5 py-3 text-left font-semibold">시장</th>
                <th className="px-5 py-3 text-right font-semibold">일봉</th>
                <th className="px-5 py-3 text-right font-semibold">주봉</th>
                <th className="px-5 py-3 text-left font-semibold">최근 일봉(수)</th>
                <th className="px-5 py-3 text-left font-semibold">최근 주봉(수)</th>
              </tr>
            </thead>
            <tbody>
              {collectedQuery.isLoading && (
                <tr><td className="px-5 py-8 text-center text-text-muted" colSpan={8}>수집 종목 조회 중</td></tr>
              )}
              {collectedQuery.isError && (
                <tr><td className="px-5 py-8 text-center text-blue-600" colSpan={8}>{collectedQuery.error.message}</td></tr>
              )}
              {!collectedQuery.isLoading && filteredSymbols.length === 0 && (
                <tr><td className="px-5 py-8 text-center text-text-muted" colSpan={8}>수집된 종목이 없습니다.</td></tr>
              )}
              {filteredSymbols.map((item) => {
                const active = selectedSymbol?.source === item.source && selectedSymbol.symbol === item.symbol;
                return (
                  <tr
                    key={`${item.source}:${item.symbol}`}
                    onClick={() => setSelectedSymbol(item)}
                    className={`cursor-pointer border-t border-border-primary transition-colors ${active ? "bg-brand-secondary" : "hover:bg-bg-input"}`}
                  >
                    <td className="px-5 py-3 font-semibold text-text-primary">{item.symbol}</td>
                    <td className="px-5 py-3 text-text-primary">{item.name || "-"}</td>
                    <td className="px-5 py-3"><Badge className="bg-bg-input text-text-secondary border-border-primary">{item.source}</Badge></td>
                    <td className="px-5 py-3 text-text-secondary">{item.market}</td>
                    <td className="px-5 py-3 text-right text-text-primary">{item.daily_bars}</td>
                    <td className="px-5 py-3 text-right text-text-primary">{item.weekly_bars}</td>
                    <td className="px-5 py-3 text-text-secondary">{item.last_daily_date ? `${item.last_daily_date} (${item.daily_bars})` : "-"}</td>
                    <td className="px-5 py-3 text-text-secondary">{item.last_weekly_date ? `${item.last_weekly_date} (${item.weekly_bars})` : "-"}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>

      <section className={`${panelClass} p-5`}>
        <div className="flex flex-wrap items-center gap-3">
          <div className="min-w-0 flex-1">
            <p className="text-xs font-semibold text-text-muted">선택 종목</p>
            <p className="truncate text-lg font-bold text-text-primary">{selectedSymbolText || "종목을 선택하세요"}</p>
          </div>
          <button
            className={`${buttonBase} bg-brand-primary text-white hover:bg-brand-primary/90`}
            disabled={!selectedSymbol || runAnalysisMutation.isPending}
            onClick={() => selectedSymbol && runAnalysisMutation.mutate(selectedSymbol.symbol)}
          >
            {runAnalysisMutation.isPending ? <RefreshCw size={16} className="animate-spin" /> : <Sparkles size={16} />}
            {runAnalysisMutation.isPending ? "분석 중..." : "해당 종목 분석하기"}
          </button>
          <button
            className={`${buttonBase} border border-border-primary bg-bg-card text-text-primary hover:bg-bg-input`}
            disabled={!selectedSymbol || analysisQuery.isPending}
            onClick={() => selectedSymbol && analysisQuery.mutate(selectedSymbol.symbol)}
          >
            결과 보기
          </button>
          <button
            className={`${buttonBase} border border-brand-primary bg-brand-secondary text-brand-primary hover:bg-brand-secondary/80`}
            disabled={!selectedSymbol || !activeWin || llmRequestMutation.isPending}
            onClick={() => {
              if (selectedSymbol && activeWin) {
                llmRequestMutation.mutate({
                  symbol: selectedSymbol.symbol,
                  window: activeWin.window,
                  interval: activeWin.interval,
                });
              }
            }}
          >
            {llmRequestMutation.isPending ? <RefreshCw size={16} className="animate-spin" /> : <Sparkles size={16} />}
            {llmRequestMutation.isPending ? "LLM 요청 중..." : "LLM 차트 분석 요청"}
          </button>
        </div>
        {runAnalysisMutation.isSuccess && (
          <p className="mt-3 text-sm text-text-secondary">
            분석 완료: 성공 {runAnalysisMutation.data.success}, 실패 {runAnalysisMutation.data.failed}, 스킵 {runAnalysisMutation.data.skipped}
          </p>
        )}
        {runAnalysisMutation.isError && <p className="mt-3 text-sm text-blue-600">{runAnalysisMutation.error.message}</p>}
        {analysisQuery.isError && <p className="mt-3 text-sm text-blue-600">{analysisQuery.error.message}</p>}
        {llmRequestMutation.isError && <p className="mt-3 text-sm text-blue-600">{llmRequestMutation.error.message}</p>}
      </section>

      {data && (
        <div className="flex flex-wrap gap-2">
          {availableWindows.map((w) => (
            <button
              key={w}
              onClick={() => setSelectedWindow(w)}
              className={`${buttonBase} border ${selectedWindow === w ? "border-brand-primary bg-brand-secondary text-brand-primary" : "border-border-primary bg-bg-card text-text-primary hover:bg-bg-input"}`}
            >
              {w}
            </button>
          ))}
        </div>
      )}

      {analysisQuery.isPending && <section className={`${panelClass} p-5 text-sm text-text-muted`}>분석 결과 조회 중</section>}

      {activeWin && (
        <div className="flex flex-col gap-4">
          <div className={`${panelClass} p-5`}>
            <div className="mb-2 flex items-center gap-2">
              <BarChart3 size={18} className="text-brand-primary" />
              <h3 className="text-[17px] font-semibold text-text-primary">
                {WINDOW_LABEL[activeWin.window] ?? activeWin.window} · {activeWin.interval === "D" ? "일봉" : "주봉"} 분석 결과
              </h3>
            </div>
            <p className="text-sm text-text-secondary">
              선택한 윈도우의 수치 분석, 지표, 패턴, 거래량, 저장 리포트 상태를 모두 보여줍니다.
            </p>
          </div>
          <WindowMetaCard win={activeWin} />
          <SummaryCard win={activeWin} onInfo={setHelpTopic} />
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
            <TrendCard win={activeWin} onInfo={setHelpTopic} />
            <LevelsCard win={activeWin} onInfo={setHelpTopic} />
            <VolumePatternCard win={activeWin} onInfo={setHelpTopic} />
          </div>
          <PatternDetailCard patterns={activeWin.technical.patterns} />
          <IndicatorTable signals={activeWin.technical.indicator_signals} onInfo={setHelpTopic} />
          <ChartNarrativeCard
            llmStatus={llmStatus}
            templateNarrative={activeWin.report.template.narrative}
            llmNarrative={activeWin.report.llm.narrative}
            error={llmRequestMutation.isError ? llmRequestMutation.error.message : null}
            onInfo={setHelpTopic}
          />
        </div>
      )}
    </div>
  );
}
