import type { EditionDetailResponse } from '../../api/types'
import { ExternalLink } from '../../components/ui/ExternalLink'
import { focusRing } from '../../components/ui/focusRing'
import { cn } from '../../lib/cn'
import { formatPrice, formatRelative, pluralRu } from '../../lib/format'
import { bestOfferId, sortOffers } from './offers'

export function OffersTable({ edition }: { edition: EditionDetailResponse }) {
  const offers = sortOffers(edition.offers)
  const bestId = bestOfferId(edition.offers, edition.bestPrice)
  const n = offers.length
  const th = 'px-3 py-2.5 text-left text-[13px] font-semibold text-muted'

  return (
    <section
      id="offers"
      aria-labelledby="offers-title"
      className="rounded-card border border-line bg-surface p-[22px]"
    >
      <h2 id="offers-title" className="mb-4 font-display text-[22px] font-medium">
        Цены в {n} {pluralRu(n, ['магазине', 'магазинах', 'магазинах'])}
      </h2>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[720px] border-collapse text-[15px]">
          <thead>
            <tr>
              <th scope="col" className={th}>Магазин</th>
              <th scope="col" className={th}>Цена</th>
              <th scope="col" className={th}>Было</th>
              <th scope="col" className={th}>Доставка</th>
              <th scope="col" className={th}>Наличие</th>
              <th scope="col" className={th}>Обновлено</th>
              <th scope="col" className={th}>
                <span className="sr-only">Действие</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {offers.map((o) => {
              const best = o.offerId != null && o.offerId === bestId
              const price = o.price ?? 0
              return (
                <tr
                  key={o.offerId ?? o.store}
                  data-best={best ? 'true' : undefined}
                  className={cn('border-t border-divider', best && 'bg-best-row')}
                >
                  <td className="px-3 py-3.5 font-semibold">{o.storeName ?? o.store}</td>
                  <td className="px-3 py-3.5 text-[17px] font-bold">{formatPrice(price)}</td>
                  <td className="px-3 py-3.5 text-muted">
                    {o.oldPrice != null && o.oldPrice > price && <s>{formatPrice(o.oldPrice)}</s>}
                  </td>
                  <td className="px-3 py-3.5">{o.deliveryText}</td>
                  <td className="px-3 py-3.5">{o.inStock === false ? 'Нет в наличии' : 'В наличии'}</td>
                  <td className="px-3 py-3.5 text-muted">
                    {o.lastSeenAt ? formatRelative(o.lastSeenAt) : ''}
                  </td>
                  <td className="px-3 py-3.5 text-right">
                    {o.url && (
                      <ExternalLink
                        href={o.url}
                        aria-label={`В магазин ${o.storeName ?? o.store}`}
                        className={cn('font-semibold text-link', focusRing)}
                      >
                        В магазин
                      </ExternalLink>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}
