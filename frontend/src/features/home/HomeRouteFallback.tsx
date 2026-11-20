// Reserves the height of the loaded home page so the footer stays below the fold
// while the route chunk loads (avoids a footer shift on the home page).
export function HomeRouteFallback() {
  return <div role="status" aria-label="Загрузка" className="min-h-[1200px]" />
}
