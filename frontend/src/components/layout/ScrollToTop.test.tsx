import { fireEvent, screen } from "@testing-library/react";
import { render } from "@testing-library/react";
import { MemoryRouter, useNavigate } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ScrollToTop } from "./ScrollToTop";
import App from "../../app/App";
import { renderWithProviders } from "../../test/render";
import { resetViewport, setViewport } from "../../test/viewport";

describe("ScrollToTop", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    resetViewport();
  });

  it("scrolls to the top when a similar book is opened", async () => {
    setViewport(1280);
    const scrollTo = vi.spyOn(window, "scrollTo").mockImplementation(() => {});
    renderWithProviders(<App />, { route: "/editions/41" });
    const card = await screen.findByRole("link", { name: /Собачье сердце/ });
    scrollTo.mockClear();
    fireEvent.click(card);
    await screen.findByRole("heading", { level: 1 });
    expect(scrollTo).toHaveBeenCalledWith(0, 0);
  });

  function Nav() {
    const navigate = useNavigate();
    return (
      <>
        <button onClick={() => navigate("/search?sort=price")}>filter</button>
        <button onClick={() => navigate("/other")}>other</button>
        <button onClick={() => navigate(-1)}>back</button>
        <ScrollToTop />
      </>
    );
  }

  it("does not scroll when only the query string changes", () => {
    const scrollTo = vi.spyOn(window, "scrollTo").mockImplementation(() => {});
    render(
      <MemoryRouter initialEntries={["/search"]}>
        <Nav />
      </MemoryRouter>,
    );
    scrollTo.mockClear();
    fireEvent.click(screen.getByText("filter"));
    expect(scrollTo).not.toHaveBeenCalled();
  });

  it("does not scroll on a back navigation to another path", () => {
    const scrollTo = vi.spyOn(window, "scrollTo").mockImplementation(() => {});
    render(
      <MemoryRouter initialEntries={["/search", "/other"]} initialIndex={1}>
        <Nav />
      </MemoryRouter>,
    );
    scrollTo.mockClear();
    fireEvent.click(screen.getByText("back"));
    expect(scrollTo).not.toHaveBeenCalled();
  });
});
