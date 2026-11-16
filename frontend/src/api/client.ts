export interface FieldError {
  field: string
  message: string
}

/** Normalised error for any non-2xx response (RFC 7807 body from the backend). */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: FieldError[]

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?')
  }

  if (response.ok) {
    return (response.status === 204 ? undefined : await response.json()) as T
  }

  const body = await response.json().catch(() => null)
  throw new ApiError(
    response.status,
    body?.detail ?? body?.title ?? `Request failed (${response.status})`,
    body?.errors ?? [],
  )
}
