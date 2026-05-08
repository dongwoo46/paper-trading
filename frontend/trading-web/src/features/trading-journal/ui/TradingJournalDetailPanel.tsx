import { useEffect, useState } from "react";
import type { JournalSentiment, TradingJournalDetailResponse } from "../../../entities/trading-journal/model/types";

type Props = {
  detail: TradingJournalDetailResponse | null;
  isLoading: boolean;
  isError: boolean;
  isSaving: boolean;
  onSave: (body: { title: string; content: string; sentiment: JournalSentiment }) => void;
};

export function TradingJournalDetailPanel({ detail, isLoading, isError, isSaving, onSave }: Props) {
  const [sentiment, setSentiment] = useState<JournalSentiment>("NEUTRAL");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  useEffect(() => {
    if (detail) {
      setSentiment(detail.sentiment);
      setTitle(detail.title);
      setContent(detail.content);
    }
  }, [detail]);

  return (
    <section className="rounded-[20px] border border-white/12 bg-bg-card p-4 sm:p-5">
      <h3 className="mb-3 text-base font-semibold">일지 상세</h3>
      {isLoading && <div className="p-4 text-sm text-text-secondary">상세를 불러오는 중...</div>}
      {isError && <div className="p-4 text-sm text-status-error">상세를 불러오지 못했습니다.</div>}
      {!isLoading && !isError && !detail && <div className="p-4 text-sm text-text-secondary">거래 일지를 선택하세요.</div>}
      {!isLoading && !isError && detail && (
        <div className="grid gap-3">
          <label className="flex flex-col gap-1.5 text-sm text-text-secondary">
            심리
            <select className="h-10 rounded-lg border border-white/12 bg-bg-input px-3 text-text-primary outline-none focus:border-brand-primary" value={sentiment} onChange={(e) => setSentiment(e.target.value as JournalSentiment)}>
              <option value="BULLISH">BULLISH</option>
              <option value="BEARISH">BEARISH</option>
              <option value="NEUTRAL">NEUTRAL</option>
              <option value="REFLECTIVE">REFLECTIVE</option>
            </select>
          </label>
          <label className="flex flex-col gap-1.5 text-sm text-text-secondary">
            제목
            <input className="h-10 rounded-lg border border-white/12 bg-bg-input px-3 text-text-primary outline-none focus:border-brand-primary" aria-label="제목" value={title} onChange={(e) => setTitle(e.target.value)} />
          </label>
          <label className="flex flex-col gap-1.5 text-sm text-text-secondary">
            내용
            <textarea className="min-h-[160px] rounded-lg border border-white/12 bg-bg-input px-3 py-2 text-text-primary outline-none focus:border-brand-primary" aria-label="내용" value={content} onChange={(e) => setContent(e.target.value)} rows={6} />
          </label>
          <button className="h-10 w-fit rounded-lg border border-brand-primary/40 bg-brand-primary/15 px-4 text-sm font-semibold text-brand-primary transition hover:bg-brand-primary/25 disabled:cursor-not-allowed disabled:opacity-50" type="button" disabled={isSaving} onClick={() => onSave({ title, content, sentiment })}>
            저장
          </button>
        </div>
      )}
    </section>
  );
}
