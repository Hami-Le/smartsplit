export function navigate(path: string, replace = false): void {
  if (replace) {
    window.history.replaceState({}, '', path)
  } else {
    window.history.pushState({}, '', path)
  }
  window.dispatchEvent(new PopStateEvent('popstate'))
}

export function currentPath(): string {
  return window.location.pathname
}
