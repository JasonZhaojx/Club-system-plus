import { getAccessToken } from '@/auth'

export interface AssistantSource {
  type: string
  id: string
  title: string
}

export interface AssistantMeta {
  answer: string
  intent: string
  sources: AssistantSource[]
}

interface StreamAssistantOptions {
  message: string
  onMeta?: (meta: AssistantMeta) => void
  onToken: (token: string) => void
  onError?: (message: string) => void
}

export async function streamAssistantChat({
  message,
  onMeta,
  onToken,
  onError,
}: StreamAssistantOptions) {
  const token = getAccessToken()
  if (!token) {
    throw new Error('请先登录后再使用 AI 助手')
  }

  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'
  const response = await fetch(`${baseUrl}/assistant/chat/stream`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ message }),
  })

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }
  if (!response.body) {
    throw new Error('AI 助手暂时不可用')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split(/\n\n/)
    buffer = events.pop() ?? ''
    for (const eventText of events) {
      handleSseEvent(eventText, { onMeta, onToken, onError })
    }
  }

  if (buffer.trim()) {
    handleSseEvent(buffer, { onMeta, onToken, onError })
  }
}

async function readErrorMessage(response: Response) {
  try {
    const payload = (await response.json()) as { message?: string }
    return payload.message || 'AI 助手暂时不可用'
  } catch {
    return 'AI 助手暂时不可用'
  }
}

function handleSseEvent(
  eventText: string,
  handlers: Pick<StreamAssistantOptions, 'onMeta' | 'onToken' | 'onError'>,
) {
  const lines = eventText.split(/\n/)
  let eventName = 'message'
  const dataLines: string[] = []

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length).trim()
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart())
    }
  }

  const data = dataLines.join('\n')
  if (!data) {
    return
  }
  if (eventName === 'meta') {
    try {
      handlers.onMeta?.(JSON.parse(data) as AssistantMeta)
    } catch {
      // Ignore malformed metadata and keep the stream alive.
    }
    return
  }
  if (eventName === 'token') {
    try {
      const payload = JSON.parse(data) as { token?: string }
      handlers.onToken(payload.token ?? '')
    } catch {
      handlers.onToken(data)
    }
    return
  }
  if (eventName === 'error') {
    handlers.onError?.(data)
  }
}
