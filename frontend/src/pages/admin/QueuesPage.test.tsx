import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { afterEach, describe, expect, it } from "vitest";
import { server } from "../../mocks/server";
import { renderAdmin } from "../../test/admin";

afterEach(() => {
  server.events.removeAllListeners();
});

function countPosts(): { count: number } {
  const log = { count: 0 };
  server.events.on("request:start", ({ request }) => {
    if (
      request.method === "POST" &&
      new URL(request.url).pathname.endsWith("/queues/dead-letter/requeue")
    ) {
      log.count += 1;
    }
  });
  return log;
}

async function openPage() {
  renderAdmin("/admin/queues");
  await screen.findByRole("heading", { level: 1, name: "Очереди RabbitMQ" });
  await screen.findByRole("heading", {
    level: 2,
    name: "Dead letter: 7 сообщений",
  });
}

const rowOf = (name: string) =>
  screen.getByText(name, { selector: "code" }).closest("tr") as HTMLElement;

describe("admin queues page", () => {
  it("lists the queues with captions and rates", async () => {
    await openPage();
    expect(
      screen.getByText(/Обновляется каждые 30 секунд/),
    ).toBeInTheDocument();
    expect(screen.getAllByRole("row")).toHaveLength(8);
    expect(within(rowOf("kr.parse.flip")).getByText("38/мин")).toBeInTheDocument();
    expect(
      within(rowOf("kr.parse.flip")).getByText("задачи на парсинг"),
    ).toBeInTheDocument();
    const dead = within(rowOf("kr.dead-letter")).getByText("7");
    expect(dead.tagName).toBe("STRONG");
    expect(dead).toHaveClass("text-danger");
    expect(
      screen.getByRole("link", {
        name: "Открыть журнал событий с уровнем ERROR",
      }),
    ).toHaveAttribute("href", "/admin/events?level=ERROR");
  });

  it("asks for confirmation and cancel sends nothing", async () => {
    const posts = countPosts();
    await openPage();
    fireEvent.click(screen.getByRole("button", { name: "Вернуть в работу" }));
    const dialog = await screen.findByRole("dialog", {
      name: "Вернуть сообщения в работу?",
    });
    expect(
      within(dialog).getByText("7 сообщений снова попадут в свои очереди."),
    ).toBeInTheDocument();
    expect(posts.count).toBe(0);
    fireEvent.click(within(dialog).getByRole("button", { name: "Отмена" }));
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(posts.count).toBe(0);
  });

  it("requeues after the confirmation and refreshes the queues", async () => {
    const posts = countPosts();
    await openPage();
    fireEvent.click(screen.getByRole("button", { name: "Вернуть в работу" }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Вернуть в работу" }),
    );
    expect(
      await screen.findByText("Возвращено 6, пропущено 1."),
    ).toBeInTheDocument();
    expect(posts.count).toBe(1);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(
      await screen.findByRole("heading", {
        level: 2,
        name: "Dead letter: 0 сообщений",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Вернуть в работу" }),
    ).toBeDisabled();
  });

  it("shows an unavailable queue as a danger alert", async () => {
    server.use(
      http.post("*/api/v1/admin/queues/dead-letter/requeue", () =>
        HttpResponse.json({ code: "UNAVAILABLE", message: "x" }, { status: 503 }),
      ),
    );
    await openPage();
    fireEvent.click(screen.getByRole("button", { name: "Вернуть в работу" }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.click(
      within(dialog).getByRole("button", { name: "Вернуть в работу" }),
    );
    expect(
      await screen.findByText("Очередь недоступна, попробуйте позже"),
    ).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Очередь недоступна, попробуйте позже",
    );
  });
});
