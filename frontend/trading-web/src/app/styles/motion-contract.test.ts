import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const sourceRoot = join(process.cwd(), "src");

describe("responsive and motion design contract", () => {
  it("turns non-essential motion into immediate feedback when reduced motion is requested", () => {
    const stylesheet = readFileSync(join(sourceRoot, "app/styles/index.css"), "utf8");

    expect(stylesheet).toContain("@media (prefers-reduced-motion: reduce)");
    expect(stylesheet).toContain("animation-duration: 0.01ms");
    expect(stylesheet).toContain("animation-iteration-count: 1");
    expect(stylesheet).toContain("transition-duration: 0.01ms");
    expect(stylesheet).toContain("scroll-behavior: auto");
  });

  it("does not move shared cards or home actions on hover", () => {
    const sharedUi = readFileSync(join(sourceRoot, "shared/ui/index.tsx"), "utf8");
    const homePage = readFileSync(join(sourceRoot, "pages/home/ui/HomePage.tsx"), "utf8");

    expect(sharedUi).not.toMatch(/hover:-translate/);
    expect(homePage).not.toMatch(/group-hover:(?:-?translate)/);
  });

  it("keeps the mobile navigation scrollable without truncating link labels", () => {
    const sidebar = readFileSync(join(sourceRoot, "shared/ui/Sidebar.tsx"), "utf8");

    expect(sidebar).toContain("overflow-y-auto");
    expect(sidebar).not.toMatch(/<span className="truncate">\{item\.label\}<\/span>/);
  });

  it("uses named design tokens instead of arbitrary shell metrics", () => {
    const shellSources = [
      "App.tsx",
      "shared/ui/Sidebar.tsx",
      "shared/ui/TopBar.tsx",
      "shared/ui/PageHeader.tsx",
      "shared/ui/RouteErrorPage.tsx",
    ].map((path) => readFileSync(join(sourceRoot, path), "utf8")).join("\n");

    expect(shellSources).not.toMatch(
      /max-w-\[1600px\]|min-h-\[45vh\]|backdrop-blur-\[2px\]|text-\[11px\]|tracking-\[\d+(?:\.\d+)?em\]/,
    );
  });

  it("uses the same 1024px desktop boundary across shell state and styles", () => {
    const app = readFileSync(join(sourceRoot, "App.tsx"), "utf8");
    const sidebar = readFileSync(join(sourceRoot, "shared/ui/Sidebar.tsx"), "utf8");
    const topBar = readFileSync(join(sourceRoot, "shared/ui/TopBar.tsx"), "utf8");

    expect(app).toContain("const DESKTOP_BREAKPOINT_PX = 1024");
    expect(app).toContain("window.innerWidth >= DESKTOP_BREAKPOINT_PX");
    expect(sidebar).toContain("const DESKTOP_BREAKPOINT_PX = 1024");
    expect(sidebar).toContain("max-lg:");
    expect(topBar).toContain("lg:hidden");
  });
});
