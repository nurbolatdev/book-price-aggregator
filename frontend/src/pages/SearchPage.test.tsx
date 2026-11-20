import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { emptySearchPage, facets, searchPage } from "../mocks/fixtures";
import {
  MOCK_ACCESS_TOKEN,
  mockUser,
  setMockSignedIn,
} from "../mocks/authFixtures";
import { server } from "../mocks/server";
import { useAuthStore } from "../stores/auth";
import { useToastStore } from "../stores/toast";
import { getLocation, renderWithProviders } from "../test/render";
import { setViewport } from "../test/viewport";
import { Route, Routes } from "react-router-dom";
import EditionPage from "./EditionPage";
import SearchPage from "./SearchPage";

const ROUTE = "/search?q=" + encodeURIComponent("Мастер и Маргарита");

describe("SearchPage", () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
  });

  it("sets the title for a query", () => {
    renderWithProviders(<SearchPage />, { route: ROUTE });
    expect(document.title).toBe("«Мастер и Маргарита» — поиск книг | KitapRadar");
  });

  it("sets the title for the catalog", () => {
    renderWithProviders(<SearchPage />, { route: "/catalog" });
    expect(document.title).toBe("Каталог книг | KitapRadar");
  });

  it("renders heading, total and result cards", async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE });
    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "«Мастер и Маргарита»",
      }),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole("status")).toHaveTextContent("Найдено 12 изданий"),
    );
    const visible = screen.getByText("Найдено 12 изданий", { selector: "p[aria-hidden]" });
    expect(visible).toHaveAttribute("aria-hidden", "true");
    const links = screen.getAllByRole("link", { name: "Мастер и Маргарита" });
    expect(links[0]).toHaveAttribute("href", "/editions/12");
    expect(screen.getAllByRole("article")).toHaveLength(3);
    const best = document.querySelector("[data-best]") as HTMLElement;
    expect(within(best).getByText(/3\s150/)).toBeInTheDocument();
  });

  it("loads the next page on demand", async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE });
    fireEvent.click(
      await screen.findByRole("button", { name: "Показать ещё 9 изданий" }),
    );
    await waitFor(() =>
      expect(screen.getAllByRole("article")).toHaveLength(12),
    );
    expect(
      screen.queryByRole("button", { name: /Показать ещё/ }),
    ).not.toBeInTheDocument();
  });

  it("restarts from page 0 with the new sort", async () => {
    const seen: string[] = [];
    server.use(
      http.get("*/api/v1/search", ({ request }) => {
        seen.push(new URL(request.url).search);
        return HttpResponse.json(emptySearchPage);
      }),
    );
    renderWithProviders(<SearchPage />, { route: ROUTE });
    await screen.findByRole("combobox", { name: "Сортировка" });
    fireEvent.change(screen.getByRole("combobox", { name: "Сортировка" }), {
      target: { value: "PRICE_ASC" },
    });
    await waitFor(() =>
      expect(seen.some((s) => s.includes("sort=PRICE_ASC"))).toBe(true),
    );
    const last = new URLSearchParams(seen[seen.length - 1]);
    expect(last.get("page")).toBe("0");
    expect(screen.getByRole("combobox", { name: "Сортировка" })).toHaveValue(
      "PRICE_ASC",
    );
  });

  it("restores the sort from the url", async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE + "&sort=PRICE_ASC" });
    expect(
      await screen.findByRole("combobox", { name: "Сортировка" }),
    ).toHaveValue("PRICE_ASC");
  });

  it("shows the empty state with filter reset and popular chips", async () => {
    server.use(
      http.get("*/api/v1/search", () => HttpResponse.json(emptySearchPage)),
    );
    renderWithProviders(<SearchPage />, { route: ROUTE + "&stores=FLIP" });
    expect(
      await screen.findByText(
        "По запросу «Мастер и Маргарита» ничего не нашли",
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Ничего не нашли");
    expect(
      screen.getByRole("button", { name: "Сбросить фильтры" }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("link", { name: "Атомные привычки" }),
    ).toBeInTheDocument();
  });

  it("shows an error block and recovers on retry", async () => {
    let fail = true;
    server.use(
      http.get("*/api/v1/search", ({ request }) => {
        if (fail)
          return HttpResponse.json(
            { code: "INTERNAL", message: "x" },
            { status: 500 },
          );
        const page = new URL(request.url).searchParams.get("page");
        return HttpResponse.json({
          content: [],
          page: Number(page),
          size: 20,
          totalElements: 0,
          totalPages: 0,
        });
      }),
    );
    renderWithProviders(<SearchPage />, { route: ROUTE });
    expect(
      await screen.findByText("Не удалось загрузить результаты."),
    ).toBeInTheDocument();
    fail = false;
    server.resetHandlers();
    fireEvent.click(screen.getByRole("button", { name: "Повторить" }));
    expect(await screen.findAllByRole("article")).toHaveLength(3);
  });

  it("sends guests to login from the bell", async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE });
    const bells = await screen.findAllByRole("link", {
      name: "Поставить маячок",
    });
    expect(bells[0]).toHaveAttribute(
      "href",
      "/login?next=%2Feditions%2F12%3Fbeacon%3D1",
    );
  });

  it("opens the beacon dialog from the bell for a signed-in user", async () => {
    setMockSignedIn(true);
    useAuthStore.getState().setSession({
      accessToken: MOCK_ACCESS_TOKEN,
      user: { ...mockUser, emailVerified: true },
    });
    const bodies: unknown[] = [];
    server.use(
      http.post("*/api/v1/beacons", async ({ request }) => {
        bodies.push(await request.json());
        return HttpResponse.json(
          { id: 99, editionId: 12, title: "Мастер и Маргарита", status: "ACTIVE" },
          { status: 201 },
        );
      }),
    );
    renderWithProviders(<SearchPage />, { route: ROUTE });
    const bells = await screen.findAllByRole("button", {
      name: "Поставить маячок",
    });
    expect(screen.queryByRole("link", { name: "Поставить маячок" })).toBeNull();
    fireEvent.click(bells[0]!);
    const dialog = await screen.findByRole("dialog", { name: "Маячок на книгу" });
    expect(within(dialog).getByText("Мастер и Маргарита", { selector: "strong" })).toBeInTheDocument();
    expect(within(dialog).getByText(/АСТ/)).toBeInTheDocument();
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Поставить маячок" }),
    );
    await waitFor(() => expect(bodies).toHaveLength(1));
    expect(bodies[0]).toMatchObject({ editionId: 12 });
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
  });

  it("tells that a beacon already stands for edition 16", async () => {
    setMockSignedIn(true);
    useAuthStore.getState().setSession({
      accessToken: MOCK_ACCESS_TOKEN,
      user: { ...mockUser, emailVerified: true },
    });
    renderWithProviders(<SearchPage />, { route: ROUTE });
    const bells = await screen.findAllByRole("button", {
      name: "Поставить маячок",
    });
    fireEvent.click(bells[1]!);
    const dialog = await screen.findByRole("dialog", { name: "Маячок на книгу" });
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Поставить маячок" }),
    );
    await waitFor(() =>
      expect(
        useToastStore
          .getState()
          .toasts.some((t) => t.message === "На эту книгу маячок уже стоит"),
      ).toBe(true),
    );
  });

  it("puts unchecked stores in the url and the next requests", async () => {
    const seen: string[] = [];
    server.use(
      http.get("*/api/v1/search", ({ request }) => {
        seen.push(new URL(request.url).search);
        return HttpResponse.json(searchPage(0));
      }),
      http.get("*/api/v1/search/facets", ({ request }) => {
        seen.push("facets" + new URL(request.url).search);
        return HttpResponse.json(facets);
      }),
    );
    renderWithProviders(<SearchPage />, { route: ROUTE });
    fireEvent.click(await screen.findByRole("checkbox", { name: /Ozon/ }));
    await waitFor(() =>
      expect(getLocation()).toContain("stores=FLIP%2CMELOMAN"),
    );
    await waitFor(() => {
      expect(
        seen.some(
          (s) =>
            !s.startsWith("facets") &&
            new URLSearchParams(s.replace(/^facets/, "")).get("stores") ===
              "FLIP,MELOMAN",
        ),
      ).toBe(true);
      expect(
        seen.some(
          (s) =>
            s.startsWith("facets") &&
            new URLSearchParams(s.replace(/^facets/, "")).get("stores") ===
              "FLIP,MELOMAN",
        ),
      ).toBe(true);
    });
  });

  it("writes inStock=false when the stock filter is unchecked", async () => {
    renderWithProviders(<SearchPage />, { route: ROUTE });
    fireEvent.click(
      await screen.findByRole("checkbox", { name: "Только в наличии" }),
    );
    await waitFor(() => expect(getLocation()).toContain("inStock=false"));
  });

  it("restores checkboxes from the url", async () => {
    renderWithProviders(<SearchPage />, {
      route: ROUTE + "&stores=FLIP,OZON&binding=PAPERBACK&inStock=false",
    });
    expect(
      await screen.findByRole("checkbox", { name: /Meloman/ }),
    ).not.toBeChecked();
    expect(screen.getByRole("checkbox", { name: /Ozon/ })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: /Мягкий/ })).toBeChecked();
    expect(
      screen.getByRole("checkbox", { name: "Только в наличии" }),
    ).not.toBeChecked();
  });

  it("renders the catalog without a query and without the relevance sort", async () => {
    const seen: URLSearchParams[] = [];
    server.use(
      http.get("*/api/v1/search", ({ request }) => {
        seen.push(new URL(request.url).searchParams);
        return HttpResponse.json(searchPage(0));
      }),
    );
    renderWithProviders(<SearchPage />, { route: "/catalog" });
    expect(
      await screen.findByRole("heading", { level: 1, name: "Каталог" }),
    ).toBeInTheDocument();
    await screen.findAllByRole("article");
    const sort = screen.getByRole("combobox", { name: "Сортировка" });
    expect(sort).toHaveValue("STORES_DESC");
    expect(
      within(sort).queryByRole("option", { name: "По релевантности" }),
    ).not.toBeInTheDocument();
    expect(seen[0]!.has("q")).toBe(false);
    expect(seen[0]!.get("sort")).toBe("STORES_DESC");
  });

  it("redirects /search without a usable query to the catalog", async () => {
    renderWithProviders(<SearchPage />, { route: "/search" });
    expect(
      await screen.findByRole("heading", { level: 1, name: "Каталог" }),
    ).toBeInTheDocument();
    expect(getLocation()).toBe("/catalog");
  });

  it("keeps the remaining filters when redirecting to the catalog", async () => {
    renderWithProviders(<SearchPage />, { route: "/search?q=a&stores=FLIP" });
    expect(
      await screen.findByRole("heading", { level: 1, name: "Каталог" }),
    ).toBeInTheDocument();
    expect(getLocation()).toBe("/catalog?stores=FLIP");
  });

  it("passes the search origin to the edition breadcrumbs", async () => {
    setViewport(1280);
    renderWithProviders(
      <Routes>
        <Route path="/search" element={<SearchPage />} />
        <Route path="/editions/:id" element={<EditionPage />} />
      </Routes>,
      { route: ROUTE },
    );
    const links = await screen.findAllByRole("link", {
      name: "Мастер и Маргарита",
    });
    fireEvent.click(links[0]!);
    const nav = await screen.findByRole("navigation", {
      name: "Хлебные крошки",
    });
    expect(
      within(nav).getByRole("link", { name: "Поиск «Мастер и Маргарита»" }),
    ).toBeInTheDocument();
  });
});
