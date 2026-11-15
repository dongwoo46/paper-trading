import type { DecimalString } from "./decimal"

/** DEC-03·FLAG-17: 차트 좌표 전용 Number 변환(소스 검사 허용 파일), 금액 되먹임 금지·라벨과 툴팁은 원문 문자열 */
export function toChartCoordinate(value: DecimalString): number {
  const coordinate = Number(value)
  if (!Number.isFinite(coordinate)) {
    throw new RangeError("차트 좌표로 바꿀 수 없는 값입니다.")
  }
  return coordinate
}
