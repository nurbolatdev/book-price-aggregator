import { fireEvent, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it } from "vitest";
import App from "../app/App";
import { MOCK_ACCESS_TOKEN, mockUser } from "../mocks/authFixtures";
import { server } from "../mocks/server";
import { useAuthStore } from "../stores/auth";
import { useToastStore } from "../stores/toast";
import { getLocation, renderWithProviders } from "../test/render";

function recordResets() {
  const bodies: unknown[] = [];
  server.use(
    http.post("*/api/v1/auth/reset-password", async ({ request }) => {
      bodies.push(await request.json());
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return bodies;
}

async function fillForm(newPassword: string, confirm: string) {
  await screen.findByRole("heading", { level: 1, name: "Новый пароль" });
  fireEvent.change(
    screen.getByLabelText("Новый пароль", { selector: "input" }),
    {
      target: { value: newPassword },
    },
  );
  fireEvent.change(screen.getByLabelText("Повторите пароль"), {
    target: { value: confirm },
  });
  fireEvent.click(screen.getByRole("button", { name: "Сохранить пароль" }));
}

describe("ResetPasswordPage", () => {
  afterEach(() => {
    useToastStore.setState({ toasts: [] });
  });

  it("rejects different passwords without a request", async () => {
    const bodies = recordResets();
    renderWithProviders(<App />, { route: "/reset-password?token=valid" });
    await fillForm("password123", "password124");
    const message = await screen.findByText("Пароли не совпадают");
    expect(
      screen.getByLabelText("Повторите пароль"),
    ).toHaveAccessibleDescription(/Пароли не совпадают/);
    expect(message).toBeInTheDocument();
    expect(bodies).toEqual([]);
  });

  it("rejects a short password", async () => {
    const bodies = recordResets();
    renderWithProviders(<App />, { route: "/reset-password?token=valid" });
    await fillForm("short", "short");
    expect(await screen.findAllByText("Минимум 8 символов")).not.toHaveLength(
      0,
    );
    expect(
      screen.getByLabelText("Новый пароль", { selector: "input" }),
    ).toHaveAttribute("aria-invalid", "true");
    expect(bodies).toEqual([]);
  });

  it("saves the password, toasts and goes to sign in", async () => {
    const bodies = recordResets();
    renderWithProviders(<App />, { route: "/reset-password?token=valid" });
    await fillForm("password123", "password123");
    await waitFor(() => expect(getLocation()).toBe("/login"));
    expect(bodies).toEqual([{ token: "valid", newPassword: "password123" }]);
    expect(useToastStore.getState().toasts.map((t) => t.message)).toContain(
      "Пароль изменён, войдите",
    );
  });

  it("clears a session held in this tab", async () => {
    recordResets();
    useAuthStore
      .getState()
      .setSession({ accessToken: MOCK_ACCESS_TOKEN, user: mockUser });
    renderWithProviders(<App />, { route: "/reset-password?token=valid" });
    await fillForm("password123", "password123");
    await waitFor(() =>
      expect(useAuthStore.getState().status).toBe("anonymous"),
    );
  });

  it("shows the expired panel for an expired link", async () => {
    renderWithProviders(<App />, { route: "/reset-password?token=expired" });
    await fillForm("password123", "password123");
    const heading = await screen.findByRole("heading", {
      level: 1,
      name: "Ссылка устарела",
    });
    expect(heading).toHaveFocus();
    expect(
      screen.getByRole("link", { name: "Запросить новую ссылку" }),
    ).toHaveAttribute("href", "/forgot-password");
  });

  it("shows the same panel for an invalid token", async () => {
    renderWithProviders(<App />, { route: "/reset-password?token=garbage" });
    await fillForm("password123", "password123");
    expect(
      await screen.findByRole("heading", { level: 1, name: "Ссылка устарела" }),
    ).toBeInTheDocument();
  });

  it("shows the panel at once without a token and sends nothing", async () => {
    const bodies = recordResets();
    renderWithProviders(<App />, { route: "/reset-password" });
    expect(
      await screen.findByRole("heading", { level: 1, name: "Ссылка устарела" }),
    ).toBeInTheDocument();
    expect(bodies).toEqual([]);
  });
});
