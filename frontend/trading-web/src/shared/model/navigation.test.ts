import { describe, expect, it } from "vitest";
import { navigationGroups, navigationItems } from "./navigation";

describe("navigation", () => {
  it("groups every existing route into the four workstation areas", () => {
    expect(navigationGroups.map((group) => group.label)).toEqual([
      "개요",
      "트레이딩",
      "시장 데이터",
      "분석",
    ]);

    expect(navigationItems.map((item) => item.to)).toEqual([
      "/",
      "/account",
      "/orders",
      "/portfolio",
      "/trading-journals",
      "/tax-summary",
      "/realtime",
      "/historical",
      "/macro",
      "/market-unified",
      "/chart-analysis",
    ]);
  });

  it("keeps route labels and descriptions unique", () => {
    expect(new Set(navigationItems.map((item) => item.label)).size).toBe(11);
    expect(navigationItems.every((item) => item.description.length > 0)).toBe(true);
  });
});
