import { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { confirmPasswordReset, sendPasswordResetCode } from '@/api/modules/auth'
import { getErrorMessage, showToast } from '@/toast'

type ResetStep = 'email' | 'confirm'

export default function ForgotPasswordView() {
  const navigate = useNavigate()
  const [step, setStep] = useState<ResetStep>('email')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function handleSendCode(event: FormEvent) {
    event.preventDefault()
    await sendCode()
  }

  async function sendCode() {
    setSubmitting(true)
    setError('')
    setNotice('')
    try {
      await sendPasswordResetCode({ email: email.trim() })
      setStep('confirm')
      setNotice('如果该邮箱已注册，验证码会发送到对应邮箱。')
    } catch (err) {
      setError(getErrorMessage(err, '验证码发送失败'))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleConfirm(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      await confirmPasswordReset({
        email: email.trim(),
        code: code.trim(),
        newPassword,
      })
      showToast('密码已重置，请使用新密码登录', 'success')
      navigate('/login', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, '密码重置失败'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <form
        className="auth-panel"
        onSubmit={step === 'email' ? handleSendCode : handleConfirm}
      >
        <h1>找回密码</h1>
        <label>
          邮箱
          <input
            autoComplete="email"
            disabled={step === 'confirm'}
            onChange={(event) => setEmail(event.target.value)}
            required
            type="email"
            value={email}
          />
        </label>
        {step === 'confirm' && (
          <>
            <label>
              验证码
              <input
                inputMode="numeric"
                maxLength={6}
                onChange={(event) => setCode(event.target.value)}
                pattern="[0-9]{6}"
                required
                value={code}
              />
            </label>
            <label>
              新密码
              <input
                autoComplete="new-password"
                minLength={8}
                onChange={(event) => setNewPassword(event.target.value)}
                required
                type="password"
                value={newPassword}
              />
            </label>
          </>
        )}
        <button disabled={submitting} type="submit">
          {submitting ? '提交中...' : step === 'email' ? '发送验证码' : '重置密码'}
        </button>
        {step === 'confirm' && (
          <button
            className="secondary-button"
            disabled={submitting}
            onClick={sendCode}
            type="button"
          >
            重新发送验证码
          </button>
        )}
        {notice && <p className="form-success">{notice}</p>}
        {error && <p className="form-error">{error}</p>}
        <p className="auth-switch">
          已想起密码？
          <Link className="inline-link" to="/login">
            返回登录
          </Link>
        </p>
      </form>
    </section>
  )
}
