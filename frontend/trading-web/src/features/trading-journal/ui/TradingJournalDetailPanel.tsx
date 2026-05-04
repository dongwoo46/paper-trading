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
    <section className="panel">
      <h3>일지 상세</h3>
      {isLoading && <div style={{ padding: 16 }}>상세를 불러오는 중...</div>}
      {isError && <div style={{ padding: 16, color: "#ef4444" }}>상세를 불러오지 못했습니다.</div>}
      {!isLoading && !isError && !detail && <div style={{ padding: 16 }}>거래 일지를 선택하세요.</div>}
      {!isLoading && !isError && detail && (
        <div style={{ display: "grid", gap: 10 }}>
          <label>
            심리
            <select value={sentiment} onChange={(e) => setSentiment(e.target.value as JournalSentiment)}>
              <option value="BULLISH">BULLISH</option>
              <option value="BEARISH">BEARISH</option>
              <option value="NEUTRAL">NEUTRAL</option>
              <option value="REFLECTIVE">REFLECTIVE</option>
            </select>
          </label>
          <label>
            제목
            <input aria-label="제목" value={title} onChange={(e) => setTitle(e.target.value)} />
          </label>
          <label>
            내용
            <textarea aria-label="내용" value={content} onChange={(e) => setContent(e.target.value)} rows={6} />
          </label>
          <button type="button" disabled={isSaving} onClick={() => onSave({ title, content, sentiment })}>
            저장
          </button>
        </div>
      )}
    </section>
  );
}
