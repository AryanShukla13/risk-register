import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from '../../api/client'

/** Maps server-side field errors onto the form; returns a banner message for everything else. */
export function applyServerErrors<T extends FieldValues>(error: unknown, setError: UseFormSetError<T>, known: string[]) {
  if (!(error instanceof ApiError)) return 'Something went wrong'
  let unmapped = error.fieldErrors.length === 0 ? error.message : ''
  for (const fe of error.fieldErrors) {
    if (known.includes(fe.field)) setError(fe.field as Path<T>, { message: fe.message })
    else unmapped = `${fe.field}: ${fe.message}`
  }
  return unmapped
}
