import { FormEvent, useEffect, useRef, useState } from 'react'
import { streamAssistantChat, type AssistantSource } from '@/api/modules/assistant'
import { getAccessToken } from '@/auth'

type ChatRole = 'assistant' | 'user'

interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  sources?: AssistantSource[]
}

const WELCOME_MESSAGE: ChatMessage = {
  id: 'welcome',
  role: 'assistant',
  content: '你好，我可以帮你查询活动、优惠券、部门介绍和常见问题。',
}

export default function AssistantWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const [error, setError] = useState('')
  const listRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, open])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const message = input.trim()
    if (!message || streaming) {
      return
    }
    if (!getAccessToken()) {
      setError('请先登录后再使用 AI 助手。')
      return
    }

    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: message,
    }
    const assistantId = crypto.randomUUID()
    setMessages((current) => [
      ...current,
      userMessage,
      { id: assistantId, role: 'assistant', content: '' },
    ])
    setInput('')
    setError('')
    setStreaming(true)

    try {
      await streamAssistantChat({
        message,
        onMeta: (meta) => {
          setMessages((current) =>
            current.map((item) =>
              item.id === assistantId ? { ...item, sources: meta.sources } : item,
            ),
          )
        },
        onToken: (token) => {
          setMessages((current) =>
            current.map((item) =>
              item.id === assistantId ? { ...item, content: item.content + token } : item,
            ),
          )
        },
        onError: (streamError) => {
          setError(streamError)
        },
      })
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'AI 助手暂时不可用')
      setMessages((current) =>
        current.map((item) =>
          item.id === assistantId && !item.content
            ? { ...item, content: '抱歉，AI 助手暂时不可用。' }
            : item,
        ),
      )
    } finally {
      setStreaming(false)
    }
  }

  return (
    <>
      <button
        aria-label={open ? '关闭 AI 助手' : '打开 AI 助手'}
        className="assistant-fab"
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        AI
      </button>
      <section className={open ? 'assistant-panel open' : 'assistant-panel'} aria-live="polite">
        <header className="assistant-header">
          <div>
            <strong>AI 助手</strong>
            <span>活动、优惠券、部门和 FAQ</span>
          </div>
          <button aria-label="关闭 AI 助手" onClick={() => setOpen(false)} type="button">
            x
          </button>
        </header>
        <div className="assistant-messages" ref={listRef}>
          {messages.map((message) => (
            <article className={`assistant-message ${message.role}`} key={message.id}>
              <p>{message.content || (streaming && message.role === 'assistant' ? '正在思考...' : '')}</p>
              {message.sources && message.sources.length > 0 && (
                <div className="assistant-sources">
                  {message.sources.slice(0, 4).map((source) => (
                    <span key={`${source.type}-${source.id}`}>{source.title}</span>
                  ))}
                </div>
              )}
            </article>
          ))}
        </div>
        {error && <p className="assistant-error">{error}</p>}
        <form className="assistant-input" onSubmit={handleSubmit}>
          <input
            disabled={streaming}
            onChange={(event) => setInput(event.target.value)}
            placeholder="问问最近活动、优惠券或部门..."
            value={input}
          />
          <button disabled={streaming || !input.trim()} type="submit">
            {streaming ? '发送中' : '发送'}
          </button>
        </form>
      </section>
    </>
  )
}
