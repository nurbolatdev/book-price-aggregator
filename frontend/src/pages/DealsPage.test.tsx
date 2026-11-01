import { fireEvent, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { drops } from "../mocks/fixtures";
import { server } from "../mocks/server";
import { getLocation, renderWithProviders } from "../test/render";
import DealsPage from "./DealsPage";

describe("DealsPage", () => {
  it("requests 48 drops and renders the cards", async () => {
    const limits: (string | null)[] = [];
    server.use(
      http.get("*/api/v1/home/drops", ({ request }) => {
        limits.push(new URL(request.url).searchParams.get("limit"));
        return HttpResponse.json(drops);
      }),
    );
    renderWithProviders(<DealsPage />, { route: "/deals" });
    expect(
      await screen.findByRole("heading", { level: 1, name: "Скидки недели" }),
    ).toBeInTheDocument();
    expect(await screen.findAllByText(/^−\d+% за 7 дней$/)).toHaveLength(
      drops.length,
    );
    expect(limits).toEqual(["48"]);
  });

  it("shows the empty state with a link to the catalog", async () => {
    server.use(http.get("*/api/v1/home/drops", () => HttpResponse.json([])));
    renderWithProviders(<DealsPage />, { route: "/deals" });
    expect(
      await screen.findByText("За неделю цены не снижались"),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Найти книгу" })).toHaveAttribute(
      "href",
      "/catalog",
    );
  });

  it("shows an error block and recovers on retry", async () => {
    server.use(
      http.get(
        "*/api/v1/home/drops",
        () => HttpResponse.json({ code: "X" }, { status: 500 }),
        { once: true },
      ),
    );
    renderWithProviders(<DealsPage />, { route: "/deals" });
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Повторить" }));
    expect(await screen.findAllByText("Мастер и Маргарита")).not.toHaveLength(
      0,
    );
    expect(getLocation()).toBe("/deals");
  });
});
