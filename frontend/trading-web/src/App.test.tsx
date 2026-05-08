import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import App from "./App";

vi.mock("./features/execution-toast/ui/ExecutionToastProvider", () => ({
  ExecutionToastProvider: () => null,
}));

describe("App routing", () => {
  it("renders market bars page header on /market-bars route", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/market-bars"]}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>
    );

    const headings = await screen.findAllByRole("heading", { name: "분봉 히스토리 차트" });
    expect(headings.length).toBeGreaterThan(0);
  });
});
