import { forwardRef, useRef, useState, type InputHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { IconButton } from './IconButton'
import { Input } from './Input'

type PasswordInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(function PasswordInput(props, ref) {
  const [visible, setVisible] = useState(false)
  const inner = useRef<HTMLInputElement | null>(null)

  const setRefs = (node: HTMLInputElement | null) => {
    inner.current = node
    if (typeof ref === 'function') ref(node)
    else if (ref) ref.current = node
  }

  return (
    <div className="relative">
      <Input {...props} ref={setRefs} type={visible ? 'text' : 'password'} inputSize="lg" trailingButton />
      <IconButton
        label={visible ? 'Скрыть пароль' : 'Показать пароль'}
        variant="plain"
        aria-pressed={visible}
        onClick={() => {
          setVisible((v) => !v)
          inner.current?.focus()
        }}
        className="absolute right-0.5 top-0.5"
      >
        {visible ? <EyeOff aria-hidden="true" size={20} /> : <Eye aria-hidden="true" size={20} />}
      </IconButton>
    </div>
  )
})
