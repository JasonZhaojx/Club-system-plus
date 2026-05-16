import { FormEvent, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { login, register } from '@/api/modules/auth'
import { canAccessAdmin, saveAuth } from '@/auth'

type AuthMode = 'login' | 'register'

export default function LoginView() {
  const navigate = useNavigate()
  const location = useLocation()
  const [mode, setMode] = useState<AuthMode>(() =>
    new URLSearchParams(location.search).get('mode') === 'register' ? 'register' : 'login',
  )
  const [username, setUsername] = useState('')
  const [nickname, setNickname] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setMode(new URLSearchParams(location.search).get('mode') === 'register' ? 'register' : 'login')
    setError('')
  }, [location.search])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const auth =
        mode === 'login'
          ? await login({ username, password })
          : await register({
              username,
              password,
              nickname: nickname || undefined,
              email: email || undefined,
            })
      saveAuth(auth)
      const redirectTo = new URLSearchParams(location.search).get('redirect')
      if (redirectTo === '/admin' && !canAccessAdmin(auth.user)) {
        navigate('/', { replace: true })
        return
      }
      navigate(redirectTo || '/', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : mode === 'login' ? '登录失败' : '注册失败')
    } finally {
      setSubmitting(false)
    }
  }

  function switchMode(nextMode: AuthMode) {
    const params = new URLSearchParams(location.search)
    if (nextMode === 'register') {
      params.set('mode', 'register')
    } else {
      params.delete('mode')
    }
    navigate(`/login${params.toString() ? `?${params.toString()}` : ''}`, { replace: true })
  }

  return (
    <section className="auth-page">
      <form className="auth-panel" onSubmit={handleSubmit}>
        <h1>{mode === 'login' ? '登录账号' : '注册账号'}</h1>
        <label>
          用户名
          <input
            autoComplete="username"
            maxLength={50}
            minLength={mode === 'register' ? 3 : undefined}
            onChange={(event) => setUsername(event.target.value)}
            required
            value={username}
          />
        </label>
        {mode === 'register' && (
          <>
            <label>
              昵称
              <input
                maxLength={50}
                onChange={(event) => setNickname(event.target.value)}
                value={nickname}
              />
            </label>
            <label>
              邮箱
              <input
                autoComplete="email"
                onChange={(event) => setEmail(event.target.value)}
                type="email"
                value={email}
              />
            </label>
          </>
        )}
        <label>
          密码
          <input
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            minLength={mode === 'register' ? 8 : undefined}
            onChange={(event) => setPassword(event.target.value)}
            required
            type="password"
            value={password}
          />
        </label>
        <button disabled={submitting} type="submit">
          {submitting ? '提交中...' : mode === 'login' ? '登录' : '注册并登录'}
        </button>
        {error && <p className="form-error">{error}</p>}
        {mode === 'login' ? (
          <p className="auth-switch">
            如果没有账号，请先
            <button className="inline-link" onClick={() => switchMode('register')} type="button">
              注册
            </button>
          </p>
        ) : (
          <button className="secondary-button" onClick={() => switchMode('login')} type="button">
            返回登录
          </button>
        )}
      </form>
    </section>
  )
}
