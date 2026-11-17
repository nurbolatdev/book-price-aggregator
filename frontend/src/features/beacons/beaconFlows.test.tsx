import { QueryClient } from "@tanstack/react-query";
import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import App from "../../app/App";
import { Toaster } from "../../components/ui/Toaster";
import { MOCK_ACCESS_TOKEN, setMockSignedIn } from "../../mocks/authFixtures";
import { server } from "../../mocks/server";
import { useAuthStore } from "../../stores/auth";
import { renderWithProviders } from "../../test/render";
import { setViewport } from "../../test/viewport";

const user = {
  id: 7,
  email: "nurbo@example.com",
  name: "Нурболат",
  role: "USER" as const,
  emailVerified: false,
  notifyFrequency: "INSTANT" as const,
  weeklyDigest: true,
};

describe("beacon flows across pages", () => {
  it("shares one cache between the edition page and my beacons", async () => {
    setViewport(1280);
    setMockSignedIn(true);
    useAuthStore
      .getState()
      .setSession({ accessToken: MOCK_ACCESS_TOKEN, user });
    const deletes: string[] = [];
    server.use(
      http.delete("*/api/v1/beacons/:id", ({ params }) => {
        deletes.push(String(params.id));
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false, gcTime: Infinity } },
    });
    const ui = (
      <>
        <App />
        <Toaster />
      </>
    );

    const first = renderWithProviders(ui, {
      route: "/editions/41",
      queryClient,
    });
    fireEvent.click(
      await screen.findByRole("button", { name: "Следить за ценой" }),
    );
    const dialog = await screen.findByRole("dialog", {
      name: "Маячок на книгу",
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Поставить маячок" }),
    );
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(
      await screen.findByRole("button", { name: "Маячок стоит · изменить" }),
    ).toBeInTheDocument();

    fireEvent.click(
      within(screen.getByRole("banner")).getAllByRole("link", {
        name: "Мои маячки",
      })[0]!,
    );
    expect(
      await screen.findByRole("tab", { name: "Все · 6" }),
    ).toBeInTheDocument();
    const rows = within(await screen.findByRole("tabpanel")).getAllByRole(
      "article",
    );
    const row = rows.find(
      (article) =>
        within(article).queryByRole("link", { name: "Мастер и Маргарита" }) &&
        !/Купить/.test(article.textContent ?? ""),
    );
    expect(row).toBeDefined();

    fireEvent.click(
      within(row!).getByRole("button", { name: "Удалить маячок" }),
    );
    expect(await screen.findByText("Маячок удалён")).toBeInTheDocument();
    const closers = screen.getAllByRole("button", { name: "Закрыть" });
    fireEvent.click(closers[closers.length - 1]!);
    await waitFor(() => expect(deletes).toHaveLength(1));
    first.unmount();

    renderWithProviders(ui, { route: "/editions/41", queryClient });
    expect(
      await screen.findByRole("button", { name: "Следить за ценой" }),
    ).toBeInTheDocument();
  });
});
