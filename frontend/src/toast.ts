export type ToastType = 'error' | 'success'

export interface ToastPayload {
  message: string
  type?: ToastType
}

export const TOAST_EVENT = 'app-toast'

export function showToast(message: string, type: ToastType = 'error') {
  window.dispatchEvent(new CustomEvent<ToastPayload>(TOAST_EVENT, { detail: { message, type } }))
}

export function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
