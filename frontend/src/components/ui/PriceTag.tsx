import { cn } from '../../lib/cn'
import { formatPrice } from '../../lib/format'

interface PriceTagProps {
  price: number
  oldPrice?: number | null
  prefix?: string
  className?: string
}

export function PriceTag({ price, oldPrice, prefix, className }: PriceTagProps) {
  return (
    <span className={cn('inline-flex items-baseline gap-2 font-bold text-ink', className)}>
      <span>
        {prefix}
        {formatPrice(price)}
      </span>
      {oldPrice != null && oldPrice > price && (
        <s className="text-[0.7em] font-normal text-muted">{formatPrice(oldPrice)}</s>
      )}
    </span>
  )
}
