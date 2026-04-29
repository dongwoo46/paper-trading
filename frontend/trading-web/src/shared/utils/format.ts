export function formatAmount(value: string | null, currency?: string): string {
  if (value === null || value === undefined) return "-";
  const formatted = parseFloat(value).toLocaleString("ko-KR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return currency ? `${formatted} ${currency}` : formatted;
}

export function formatRate(value: string | null): string {
  if (value === null || value === undefined) return "-";
  return `${parseFloat(value).toFixed(2)}%`;
}
