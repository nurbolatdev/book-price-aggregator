import { useEffect, useState, type KeyboardEvent } from 'react'
import { Input } from '../../components/ui/Input'

interface PriceRangeProps {
  minPrice?: number
  maxPrice?: number
  onCommit: (patch: { minPrice?: number; maxPrice?: number }) => void
  idPrefix?: string
}

function toText(value?: number): string {
  return value === undefined ? '' : String(value)
}

function toNumber(text: string): number | undefined {
  const clean = text.replace(/\s/g, '')
  return /^\d+$/.test(clean) ? Number(clean) : undefined
}

export function PriceRange({ minPrice, maxPrice, onCommit, idPrefix = 'price' }: PriceRangeProps) {
  const [min, setMin] = useState(toText(minPrice))
  const [max, setMax] = useState(toText(maxPrice))

  useEffect(() => setMin(toText(minPrice)), [minPrice])
  useEffect(() => setMax(toText(maxPrice)), [maxPrice])

  function commit() {
    const nextMin = toNumber(min)
    const nextMax = toNumber(max)
    if (nextMin === minPrice && nextMax === maxPrice) {
      setMin(toText(minPrice))
      setMax(toText(maxPrice))
      return
    }
    onCommit({ minPrice: nextMin, maxPrice: nextMax })
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') {
      e.preventDefault()
      commit()
    }
  }

  return (
    <div className="flex items-center gap-2">
      <label htmlFor={`${idPrefix}-min`} className="text-[13px] text-muted">
        от
      </label>
      <Input
        id={`${idPrefix}-min`}
        inputMode="numeric"
        placeholder="0"
        value={min}
        onChange={(e) => setMin(e.target.value)}
        onBlur={commit}
        onKeyDown={onKeyDown}
      />
      <label htmlFor={`${idPrefix}-max`} className="text-[13px] text-muted">
        до
      </label>
      <Input
        id={`${idPrefix}-max`}
        inputMode="numeric"
        placeholder="10 000"
        value={max}
        onChange={(e) => setMax(e.target.value)}
        onBlur={commit}
        onKeyDown={onKeyDown}
      />
    </div>
  )
}
