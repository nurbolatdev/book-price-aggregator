import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it } from "vitest";
import { server } from "../../mocks/server";
import { useToastStore } from "../../stores/toast";
import { renderAdmin } from "../../test/admin";

afterEach(() => {
  server.events.removeAllListeners();
  useToastStore.setState({ toasts: [] });
});

interface Logged {
  method: string;
  path: string;
  body: string;
}

function logRequests(): Logged[] {
  const log: Logged[] = [];
  server.events.on("request:start", async ({ request }) => {
    const url = new URL(request.url);
    if (!url.pathname.includes("/admin/")) return;
    log.push({
      method: request.method,
      path: url.pathname,
      body: await request.clone().text(),
    });
  });
  return log;
}

async function openPage() {
  renderAdmin("/admin/parsers");
  await screen.findByRole("heading", { level: 1, name: "Парсеры и магазины" });
  await screen.findByRole("switch", { name: "Магазин Flip.kz" });
}

const rowOf = (name: string) =>
  screen.getByRole("cell", { name }).closest("tr") as HTMLElement;

describe("admin parsers page", () => {
  it("lists the stores with switches and breaker state", async () => {
    await openPage();
    expect(screen.getAllByRole("row")).toHaveLength(6);
    for (const name of ["Flip.kz", "Meloman", "Ozon"]) {
      expect(
        screen.getByRole("switch", { name: `Магазин ${name}` }),
      ).toHaveAttribute("aria-checked", "true");
    }
    for (const name of ["Kaspi.kz", "Wildberries"]) {
      expect(
        screen.getByRole("switch", { name: `Магазин ${name}` }),
      ).toHaveAttribute("aria-checked", "false");
    }
    expect(within(rowOf("Flip.kz")).getByText("закрыт")).toBeInTheDocument();
    expect(within(rowOf("Ozon")).getByText("открыт")).toBeInTheDocument();
  });

  it("asks for confirmation before switching a store off, and cancel keeps it on", async () => {
    const log = logRequests();
    await openPage();
    const toggle = screen.getByRole("switch", { name: "Магазин Meloman" });
    toggle.focus();
    fireEvent.click(toggle);
    const dialog = await screen.findByRole("dialog", {
      name: "Выключить Meloman?",
    });
    expect(
      within(dialog).getByText(
        "Магазин пропадёт из поиска и фильтров, обход остановится. Цены сохранятся.",
      ),
    ).toBeInTheDocument();
    expect(log.filter((r) => r.method === "PATCH")).toHaveLength(0);
    fireEvent.click(within(dialog).getByRole("button", { name: "Отмена" }));
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(toggle).toHaveAttribute("aria-checked", "true");
    expect(toggle).toHaveFocus();
    expect(log.filter((r) => r.method === "PATCH")).toHaveLength(0);
  });

  it("switches a store off after the confirmation", async () => {
    const log = logRequests();
    await openPage();
    fireEvent.click(screen.getByRole("switch", { name: "Магазин Meloman" }));
    const dialog = await screen.findByRole("dialog", {
      name: "Выключить Meloman?",
    });
    fireEvent.click(within(dialog).getByRole("button", { name: "Выключить" }));
    expect(
      await screen.findByText("Магазин Meloman выключен"),
    ).toBeInTheDocument();
    const patches = log.filter((r) => r.method === "PATCH");
    expect(patches).toHaveLength(1);
    expect(JSON.parse(patches[0]?.body ?? '')).toEqual({ enabled: false });
    await waitFor(() =>
      expect(
        within(rowOf("Meloman")).getByText("Выключен"),
      ).toBeInTheDocument(),
    );
    expect(
      within(rowOf("Meloman")).getByRole("button", { name: "Запустить" }),
    ).toBeDisabled();
  });

  it("switches a store on at once", async () => {
    const log = logRequests();
    await openPage();
    fireEvent.click(screen.getByRole("switch", { name: "Магазин Kaspi.kz" }));
    expect(
      await screen.findByText("Магазин Kaspi.kz включён"),
    ).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    const patches = log.filter((r) => r.method === "PATCH");
    expect(patches).toHaveLength(1);
    expect(JSON.parse(patches[0]?.body ?? '')).toEqual({ enabled: true });
  });

  it("queues a single parser run", async () => {
    await openPage();
    fireEvent.click(
      within(rowOf("Flip.kz")).getByRole("button", { name: "Запустить" }),
    );
    expect(await screen.findByText("Задача в очереди")).toBeInTheDocument();
  });

  it("shows a conflict as a row message without a toast", async () => {
    server.use(
      http.post("*/api/v1/admin/parsers/FLIP/run", () =>
        HttpResponse.json({ code: "CONFLICT", message: "x" }, { status: 409 }),
      ),
    );
    await openPage();
    fireEvent.click(
      within(rowOf("Flip.kz")).getByRole("button", { name: "Запустить" }),
    );
    expect(
      await within(rowOf("Flip.kz")).findByText(
        "Магазин выключен — сначала включите его",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText("Задача в очереди")).not.toBeInTheDocument();
  });

  it("shows a queue outage in the store row", async () => {
    server.use(
      http.post("*/api/v1/admin/parsers/OZON/run", () =>
        HttpResponse.json(
          { code: "UNAVAILABLE", message: "x" },
          { status: 503 },
        ),
      ),
    );
    await openPage();
    fireEvent.click(
      within(rowOf("Ozon")).getByRole("button", { name: "Запустить" }),
    );
    expect(
      await within(rowOf("Ozon")).findByText(
        "Очередь недоступна, попробуйте позже",
      ),
    ).toBeInTheDocument();
  });

  it("runs every enabled parser and reports a partial result", async () => {
    const log = logRequests();
    server.use(
      http.post("*/api/v1/admin/parsers/OZON/run", () =>
        HttpResponse.json(
          { code: "UNAVAILABLE", message: "x" },
          { status: 503 },
        ),
      ),
    );
    await openPage();
    fireEvent.click(
      screen.getByRole("button", { name: "Запустить все включённые" }),
    );
    expect(
      await screen.findByText("Поставлено в очередь: 2 из 3"),
    ).toBeInTheDocument();
    expect(log.filter((r) => r.method === "POST")).toHaveLength(3);
  });
});
