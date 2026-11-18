import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { server } from "../../mocks/server";
import { renderAdmin } from "../../test/admin";
import { getLocation } from "../../test/render";

// jsdom has no layout: give elements a size so the virtualizer sees a viewport and fixed-height rows.
const sizeProps = ["clientHeight", "clientWidth", "offsetHeight"] as const;
const saved = sizeProps.map(
  (k) =>
    [k, Object.getOwnPropertyDescriptor(HTMLElement.prototype, k)] as const,
);
const savedObserver = globalThis.ResizeObserver;

beforeEach(() => {
  Object.defineProperty(HTMLElement.prototype, "clientHeight", {
    configurable: true,
    value: 640,
  });
  Object.defineProperty(HTMLElement.prototype, "clientWidth", {
    configurable: true,
    value: 800,
  });
  Object.defineProperty(HTMLElement.prototype, "offsetHeight", {
    configurable: true,
    value: 48,
  });
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
});

afterEach(() => {
  server.events.removeAllListeners();
  for (const [k, d] of saved)
    if (d) Object.defineProperty(HTMLElement.prototype, k, d);
  globalThis.ResizeObserver = savedObserver;
});

function logRequests(part: string): URL[] {
  const log: URL[] = [];
  server.events.on("request:start", ({ request }) => {
    const url = new URL(request.url);
    if (url.pathname.endsWith(part)) log.push(url);
  });
  return log;
}

const rowButtons = () =>
  screen
    .queryAllByRole("button", { expanded: false })
    .concat(screen.queryAllByRole("button", { expanded: true }));

describe("EventsPage", () => {
  it("renders the journal head, virtualized rows and the zero-results section", async () => {
    renderAdmin("/admin/events");
    expect(
      await screen.findByRole("heading", { level: 1, name: "Журнал событий" }),
    ).toBeInTheDocument();
    expect(
      await screen.findByText("Flip.kz: обновлено 1 284 предложения"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Ozon: таймаут 3 раза подряд, circuit breaker открыт"),
    ).toBeInTheDocument();
    const first = rowButtons()[0];
    if (!first) throw new Error("no rows");
    expect(within(first).getByText("INFO")).toBeInTheDocument();
    expect(within(first).getByText("parser")).toBeInTheDocument();
    expect(rowButtons().length).toBeLessThan(120);
  });

  it("filters by level through the URL", async () => {
    const log = logRequests("/admin/events");
    renderAdmin("/admin/events");
    await screen.findByText("Flip.kz: обновлено 1 284 предложения");
    fireEvent.click(screen.getByRole("button", { name: "ERROR" }));
    await waitFor(() =>
      expect(getLocation()).toBe("/admin/events?level=ERROR"),
    );
    await waitFor(() =>
      expect(log.some((u) => u.searchParams.get("level") === "ERROR")).toBe(
        true,
      ),
    );
    await waitFor(() => {
      const badges = rowButtons().map(
        (b) => within(b).getAllByText(/^(INFO|WARN|ERROR)$/)[0]?.textContent,
      );
      expect(badges.length).toBeGreaterThan(0);
      expect(new Set(badges)).toEqual(new Set(["ERROR"]));
    });
  });

  it("opens with the level from the URL", async () => {
    renderAdmin("/admin/events?level=WARN");
    expect(
      await screen.findByRole("button", { name: "WARN", pressed: true }),
    ).toBeInTheDocument();
  });

  it("expands and collapses the details", async () => {
    renderAdmin("/admin/events");
    const message = await screen.findByText(
      "Flip.kz: обновлено 1 284 предложения",
    );
    const row = message.closest("button");
    if (!row) throw new Error("no row button");
    fireEvent.click(row);
    expect(row).toHaveAttribute("aria-expanded", "true");
    expect(
      document.getElementById(row.getAttribute("aria-controls") ?? "")
        ?.textContent,
    ).toContain('"offers": 1284');
    fireEvent.click(row);
    expect(row).toHaveAttribute("aria-expanded", "false");
    expect(document.querySelector("pre")).toBeNull();
  });

  it("loads the next page by itself", async () => {
    const item = (id: number) => ({
      id,
      createdAt: new Date().toISOString(),
      level: "INFO",
      source: "parser",
      message: `событие ${id}`,
      details: {},
    });
    const log: URL[] = [];
    server.use(
      http.get("*/api/v1/admin/events", ({ request }) => {
        const url = new URL(request.url);
        log.push(url);
        const page = Number(url.searchParams.get("page") ?? 0);
        return HttpResponse.json({
          content: page === 0 ? [item(3), item(2), item(1)] : [item(0)],
          page,
          size: 50,
          totalElements: 4,
          totalPages: 2,
        });
      }),
    );
    renderAdmin("/admin/events");
    expect(await screen.findByText("событие 0")).toBeInTheDocument();
    expect(log.map((u) => u.searchParams.get("page"))).toEqual(["0", "1"]);
  });

  it("offers a retry when the next page fails", async () => {
    const item = (id: number) => ({
      id,
      createdAt: new Date().toISOString(),
      level: "INFO",
      source: "parser",
      message: `событие ${id}`,
      details: {},
    });
    let failNext = true;
    const log: number[] = [];
    server.use(
      http.get("*/api/v1/admin/events", ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get("page") ?? 0);
        log.push(page);
        if (page === 1 && failNext) {
          return HttpResponse.json(
            { code: "INTERNAL", message: "boom" },
            { status: 500 },
          );
        }
        return HttpResponse.json({
          content: page === 0 ? [item(3), item(2), item(1)] : [item(0)],
          page,
          size: 50,
          totalElements: 4,
          totalPages: 2,
        });
      }),
    );
    renderAdmin("/admin/events");
    const retry = await screen.findByRole("button", { name: "Повторить" });
    expect(screen.getByText("событие 3")).toBeInTheDocument();
    await new Promise((r) => setTimeout(r, 100));
    expect(log.filter((p) => p === 1).length).toBeLessThanOrEqual(2);
    failNext = false;
    fireEvent.click(retry);
    expect(await screen.findByText("событие 0")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Повторить" })).toBeNull();
  });

  it("refreshes on demand", async () => {
    const log = logRequests("/admin/events");
    renderAdmin("/admin/events");
    await screen.findByText("Flip.kz: обновлено 1 284 предложения");
    const before = log.length;
    fireEvent.click(screen.getByRole("button", { name: "Обновить" }));
    await waitFor(() => expect(log.length).toBeGreaterThan(before));
  });

  it("shows zero-result queries and switches the period", async () => {
    const log = logRequests("/admin/search/zero-results");
    renderAdmin("/admin/events");
    const section = (
      await screen.findByRole("heading", { name: "Запросы без результатов" })
    ).closest("section");
    if (!section) throw new Error("no section");
    expect(section).toHaveAttribute("id", "zero");
    expect(
      within(section).getByRole("button", { name: "7 дней", pressed: true }),
    ).toBeInTheDocument();
    expect(
      await within(section).findByText("гарри поттер и дары смерти"),
    ).toBeInTheDocument();
    expect(within(section).getAllByRole("listitem")).toHaveLength(4);
    expect(within(section).getByText("14")).toBeInTheDocument();
    fireEvent.click(within(section).getByRole("button", { name: "24 ч" }));
    await waitFor(() =>
      expect(getLocation()).toBe("/admin/events?zeroPeriod=24h"),
    );
    await waitFor(() =>
      expect(within(section).getAllByRole("listitem")).toHaveLength(2),
    );
    expect(log[log.length - 1]?.searchParams.get("period")).toBe("24h");
  });
});
