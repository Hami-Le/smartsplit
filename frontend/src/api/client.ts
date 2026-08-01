export type ApiEnvelope<T> = {
  success: boolean
  data: T
  timestamp: string
}

export type ApiErrorPayload = {
  success: false
  code: string
  message: string
  errors?: Record<string, string>
  timestamp?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fields: Record<string, string>

  constructor(status: number, payload?: Partial<ApiErrorPayload>) {
    super(payload?.message ?? `Yêu cầu thất bại với HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.code = payload?.code ?? 'HTTP_ERROR'
    this.fields = payload?.errors ?? {}
  }
}

const AUTH_STORAGE_KEY = 'smartsplit.auth'

function getAccessToken(): string | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as { accessToken?: string }
    return parsed.accessToken ?? null
  } catch {
    return null
  }
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const token = getAccessToken()
  const headers = new Headers(init.headers)

  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`/api${path}`, { ...init, headers })
  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const payload = text ? (JSON.parse(text) as unknown) : undefined

  if (!response.ok) {
    if (response.status === 401 && token) {
      window.dispatchEvent(new CustomEvent('smartsplit:auth-expired'))
    }
    throw new ApiError(response.status, payload as Partial<ApiErrorPayload>)
  }

  return (payload as ApiEnvelope<T>).data
}


export type DownloadedFile = {
  blob: Blob
  fileName: string
}

export async function apiDownload(path: string): Promise<DownloadedFile> {
  const token = getAccessToken()
  const headers = new Headers()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`/api${path}`, { headers })
  if (!response.ok) {
    const text = await response.text()
    let payload: Partial<ApiErrorPayload> | undefined
    try {
      payload = text ? JSON.parse(text) as Partial<ApiErrorPayload> : undefined
    } catch {
      payload = undefined
    }
    if (response.status === 401 && token) {
      window.dispatchEvent(new CustomEvent('smartsplit:auth-expired'))
    }
    throw new ApiError(response.status, payload)
  }

  const disposition = response.headers.get('Content-Disposition') ?? ''
  const fileNameMatch = disposition.match(/filename="?([^";]+)"?/i)
  return {
    blob: await response.blob(),
    fileName: fileNameMatch?.[1] ?? 'smartsplit-report',
  }
}
