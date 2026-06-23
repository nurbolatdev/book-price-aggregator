const SOURCE_LABELS = {
  google_books: 'Google Books',
  kaspi: 'Kaspi',
  ozon: 'Ozon',
}

export default function OffersTable({ offers }) {
  if (!offers || offers.length === 0) {
    return (
      <p className="text-sm text-gray-400 py-4">Предложений пока нет.</p>
    )
  }

  const sorted = [...offers].sort((a, b) => a.price - b.price)

  return (
    <div className="overflow-hidden rounded-xl border border-gray-100">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-gray-50 text-gray-500 text-left">
            <th className="px-4 py-3 font-medium">Магазин</th>
            <th className="px-4 py-3 font-medium">Цена</th>
            <th className="px-4 py-3 font-medium">Наличие</th>
            <th className="px-4 py-3 font-medium"></th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {sorted.map((offer, idx) => (
            <tr key={offer.id} className={idx === 0 ? 'bg-blue-50' : 'bg-white hover:bg-gray-50'}>
              <td className="px-4 py-3 font-medium text-gray-800">
                {idx === 0 && (
                  <span className="mr-2 text-xs bg-blue-600 text-white px-1.5 py-0.5 rounded">
                    Лучшая
                  </span>
                )}
                {SOURCE_LABELS[offer.source] || offer.source}
              </td>
              <td className="px-4 py-3">
                <span className="font-bold text-gray-900">
                  {Number(offer.price).toLocaleString('ru-RU')} ₸
                </span>
                {offer.originalPrice && (
                  <span className="ml-2 text-xs text-gray-400 line-through">
                    {Number(offer.originalPrice).toLocaleString('ru-RU')} ₸
                  </span>
                )}
              </td>
              <td className="px-4 py-3">
                {offer.inStock ? (
                  <span className="text-green-600 font-medium">В наличии</span>
                ) : (
                  <span className="text-gray-400">Нет в наличии</span>
                )}
              </td>
              <td className="px-4 py-3 text-right">
                <a
                  href={offer.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:underline text-xs font-medium"
                >
                  Купить →
                </a>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
