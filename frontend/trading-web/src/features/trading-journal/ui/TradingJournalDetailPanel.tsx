import { useState } from "react";
import type { JournalSentiment, TradingJournalDetailResponse } from "../../../entities/trading-journal/model/types";
import { Alert, AlertDescription } from "@/shared/ui/shadcn/alert";
import { Button } from "@/shared/ui/shadcn/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/shadcn/card";
import { Input } from "@/shared/ui/shadcn/input";
import { Label } from "@/shared/ui/shadcn/label";
import { NativeSelect, NativeSelectOption } from "@/shared/ui/shadcn/native-select";
import { Skeleton } from "@/shared/ui/shadcn/skeleton";
import { Textarea } from "@/shared/ui/shadcn/textarea";

type Props = {
  detail: TradingJournalDetailResponse | null;
  isLoading: boolean;
  isError: boolean;
  isSaving: boolean;
  onSave: (body: { title: string; content: string; sentiment: JournalSentiment }) => void;
};

function TradingJournalEditor({ detail, isSaving, onSave }: Pick<Props, "detail" | "isSaving" | "onSave"> & { detail: TradingJournalDetailResponse }) {
  const [sentiment, setSentiment] = useState<JournalSentiment>(detail.sentiment);
  const [title, setTitle] = useState(detail.title);
  const [content, setContent] = useState(detail.content);

  return (
    <div className="grid gap-3">
      <div className="grid gap-1.5">
        <Label htmlFor="journal-sentiment">심리</Label>
        <NativeSelect id="journal-sentiment" className="w-full" value={sentiment} onChange={(e) => setSentiment(e.target.value as JournalSentiment)}>
          <NativeSelectOption value="BULLISH">BULLISH</NativeSelectOption>
          <NativeSelectOption value="BEARISH">BEARISH</NativeSelectOption>
          <NativeSelectOption value="NEUTRAL">NEUTRAL</NativeSelectOption>
          <NativeSelectOption value="REFLECTIVE">REFLECTIVE</NativeSelectOption>
        </NativeSelect>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="journal-title">제목</Label>
        <Input id="journal-title" aria-label="제목" value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="journal-content">내용</Label>
        <Textarea id="journal-content" className="min-h-40" aria-label="내용" value={content} onChange={(e) => setContent(e.target.value)} rows={6} />
      </div>
      <Button className="w-fit" type="button" disabled={isSaving} onClick={() => onSave({ title, content, sentiment })}>
        저장
      </Button>
    </div>
  );
}

export function TradingJournalDetailPanel({ detail, isLoading, isError, isSaving, onSave }: Props) {
  return (
    <Card>
      <CardHeader><CardTitle>일지 상세</CardTitle></CardHeader>
      <CardContent>
      {isLoading && <Skeleton className="h-48 w-full" aria-label="거래 일지 상세 로딩 중" />}
      {isError && <Alert variant="destructive"><AlertDescription>상세를 불러오지 못했습니다.</AlertDescription></Alert>}
      {!isLoading && !isError && !detail && <Alert><AlertDescription>거래 일지를 선택하세요.</AlertDescription></Alert>}
      {!isLoading && !isError && detail && (
        <TradingJournalEditor key={detail.journalId} detail={detail} isSaving={isSaving} onSave={onSave} />
      )}
      </CardContent>
    </Card>
  );
}
