export type HealthResponse = {
  success: boolean
  data: {
    status: string
    service: string
  }
  timestamp: string
}

export async function getHealth(): Promise<HealthResponse> {
  const response = await fetch('/api/health')
  if (!response.ok) {
    throw new Error(`Backend trả về HTTP ${response.status}`)
  }
  return response.json() as Promise<HealthResponse>
}
