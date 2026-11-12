import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "../app/App";
import { server } from "../mocks/server";
import { getLocation, renderWithProviders } from "../test/render";

async function submitEmail(email = "nurbo@example.com") {
  await screen.findByRole("heading", {
    level: 1,
    name: "Восстановление пароля",
  });
  fireEvent.change(screen.getByLabelText("Email"), {
    target: { value: email },
  });
  fireEvent.click(screen.getByRole("button", { name: /^Отправить ссылку/ }));
}

describe("ForgotPasswordPage", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("sends the address once and shows the same panel", async () => {
    const bodies: unknown[] = [];
    server.use(
      http.post("*/api/v1/auth/forgot-password", async ({ request }) => {
        bodies.push(await request.json());
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(<App />, { route: "/forgot-password" });
    await submitEmail();
    const heading = await screen.findByRole("heading", {
      level: 1,
      name: "Проверьте почту",
    });
    expect(heading).toHaveFocus();
    expect(bodies).toEqual([{ email: "nurbo@example.com" }]);
    expect(screen.getByText("nurbo@example.com").tagName).toBe("STRONG");
    expect(
      screen.getByText(/Ответ одинаковый для любого адреса/),
    ).toBeInTheDocument();
  });

  it("shows the same panel for an unknown address", async () => {
    renderWithProviders(<App />, { route: "/forgot-password" });
    await submitEmail("nobody@example.com");
    expect(
      await screen.findByRole("heading", { level: 1, name: "Проверьте почту" }),
    ).toBeInTheDocument();
    expect(screen.getByText("nobody@example.com")).toBeInTheDocument();
  });

  it("validates the email before sending", async () => {
    renderWithProviders(<App />, { route: "/forgot-password" });
    await submitEmail("abc");
    expect(await screen.findByText(/Проверьте email/)).toBeInTheDocument();
  });

  it("counts down after a rate limit", async () => {
    server.use(
      http.post("*/api/v1/auth/forgot-password", () =>
        HttpResponse.json(
          {
            status: 429,
            code: "RATE_LIMITED",
            message: "Слишком много запросов",
          },
          { status: 429, headers: { "Retry-After": "30" } },
        ),
      ),
    );
    renderWithProviders(<App />, { route: "/forgot-password" });
    await submitEmail();
    expect(await screen.findByText(/Подождите 30 секунд/)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Отправить ссылку · 0:30" }),
    ).toBeDisabled();
  });

  it("links back to sign in", async () => {
    renderWithProviders(<App />, { route: "/forgot-password" });
    fireEvent.click(
      await screen.findByRole("link", { name: "Вернуться ко входу" }),
    );
    await waitFor(() => expect(getLocation()).toBe("/login"));
    act(() => undefined);
  });
});
