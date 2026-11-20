import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it } from "vitest";
import { server } from "../../mocks/server";
import { renderAdmin } from "../../test/admin";
import { getLocation } from "../../test/render";

afterEach(() => {
  server.events.removeAllListeners();
});

function logRequests(path: string): URL[] {
  const urls: URL[] = [];
  server.events.on("request:start", ({ request }) => {
    const url = new URL(request.url);
    if (url.pathname.endsWith(path)) urls.push(url);
  });
  return urls;
}

describe("admin overview", () => {
  it("shows the header, KPIs and service cards", async () => {
    renderAdmin("/admin");
    expect(
      await screen.findByRole("heading", { level: 1, name: "Обзор системы" }),
    ).toBeInTheDocument();
    expect(await screen.findByText(/последнее обновление/)).toBeInTheDocument();
    expect(await screen.findByText("+3 за 24 ч")).toBeInTheDocument();
    expect(screen.getByText("1 842 / 4 105")).toBeInTheDocument();
    const services = screen.getByRole("region", { name: "Состояние сервисов" });
    expect(
      await within(services).findByText("Backend API"),
    ).toBeInTheDocument();
    expect(
      within(services).getByText("p95 ответа: 184 мс"),
    ).toBeInTheDocument();
  });

  it("queues every enabled parser from «Запустить все»", async () => {
    renderAdmin("/admin");
    const section = await screen.findByRole("region", {
      name: "Парсеры магазинов",
    });
    await within(section).findByText("Flip.kz");
    fireEvent.click(
      within(section).getByRole("button", { name: "Запустить все" }),
    );
    expect(
      await screen.findByText("Поставлено в очередь: 3 из 3"),
    ).toBeInTheDocument();
  });

  it("switches the period through the URL and refetches stats and top queries", async () => {
    const stats = logRequests("/admin/stats");
    const top = logRequests("/admin/search/top");
    renderAdmin("/admin");
    await screen.findByText("+3 за 24 ч");
    fireEvent.click(screen.getByRole("button", { name: "7 дней" }));
    await waitFor(() => expect(getLocation()).toBe("/admin?period=7d"));
    expect(await screen.findByText("+14 за 7 дней")).toBeInTheDocument();
    await waitFor(() => {
      expect(stats.some((u) => u.searchParams.get("period") === "7d")).toBe(
        true,
      );
      expect(top.some((u) => u.searchParams.get("period") === "7d")).toBe(true);
    });
  });

  it("lists queues with their processing rate", async () => {
    renderAdmin("/admin");
    const section = await screen.findByRole("region", {
      name: "Очереди RabbitMQ",
    });
    const flip = (await within(section).findByText("kr.parse.flip")).closest(
      "li",
    ) as HTMLElement;
    expect(within(flip).getByText("38/мин обработано")).toBeInTheDocument();
    const dead = within(section)
      .getByText("kr.dead-letter")
      .closest("li") as HTMLElement;
    expect(within(dead).getByText("требуют внимания")).toBeInTheDocument();
  });

  it("shows the five latest events", async () => {
    renderAdmin("/admin");
    const section = await screen.findByRole("region", {
      name: "Последние события",
    });
    expect(
      await within(section).findByText("Flip.kz: обновлено 1 284 предложения"),
    ).toBeInTheDocument();
    expect(within(section).getAllByRole("listitem")).toHaveLength(5);
  });

  it("renders the activity chart summary and the zero-results link", async () => {
    renderAdmin("/admin");
    expect(
      await screen.findByRole("img", {
        name: /Поиски и новые маячки за 14 дней: всего поисков/,
      }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("link", { name: "смотреть список" }),
    ).toHaveAttribute("href", "/admin/events#zero");
    expect(
      await screen.findByText(/Мастер и Маргарита — 212/),
    ).toBeInTheDocument();
  });

  it("keeps the other sections when stats fail", async () => {
    server.use(
      http.get("*/api/v1/admin/stats", () =>
        HttpResponse.json(
          { code: "INTERNAL_ERROR", message: "boom" },
          { status: 500 },
        ),
      ),
    );
    renderAdmin("/admin");
    expect(
      await screen.findByRole("button", { name: "Повторить" }),
    ).toBeInTheDocument();
    const services = screen.getByRole("region", { name: "Состояние сервисов" });
    expect(
      await within(services).findByText("Backend API"),
    ).toBeInTheDocument();
  });
});
