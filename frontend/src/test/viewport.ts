type Width = 390 | 1280

function stub(width: Width) {
  window.matchMedia = (query: string) => {
    const match = /\(min-width:\s*(\d+)px\)/.exec(query)
    const matches = match ? Number(match[1]) <= width : false
    return {
      matches,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    } as MediaQueryList
  }
}

export function setViewport(width: Width): void {
  stub(width)
}

export function resetViewport(): void {
  stub(390)
}
