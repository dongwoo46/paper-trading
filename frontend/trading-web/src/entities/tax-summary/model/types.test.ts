import { describe, expect, it } from "vitest";
import { mapTaxSummaryStatusLabel } from "./types";

describe("mapTaxSummaryStatusLabel", () => {
  it("maps server status to korean labels", () => {
    expect(mapTaxSummaryStatusLabel("READY")).toBe("준비됨");
    expect(mapTaxSummaryStatusLabel("RUNNING")).toBe("계산 중");
    expect(mapTaxSummaryStatusLabel("FAILED")).toBe("실패");
  });
});
