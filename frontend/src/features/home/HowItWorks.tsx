const steps = [
  {
    n: '01',
    title: 'Найдите книгу',
    text: 'По названию, автору или ISBN. Разные издания одной книги собраны вместе.',
  },
  {
    n: '02',
    title: 'Сравните цены',
    text: 'Цена, наличие и доставка во всех магазинах на одном экране, плюс история цены.',
  },
  {
    n: '03',
    title: 'Поставьте маячок',
    text: 'Укажите желаемую цену — мы проверяем магазины каждые 6 часов и пишем на email.',
  },
]

export function HowItWorks() {
  return (
    <section className="mx-auto w-full max-w-page px-4 pb-8 pt-2 md:px-6 md:pb-14 md:pt-6">
      <h2 className="mb-4 font-display text-lg font-medium md:mb-6 md:text-[26px]">Как это работает</h2>
      <div className="grid gap-5 max-md:gap-0 max-md:divide-y max-md:divide-divider max-md:rounded-[14px] max-md:border max-md:border-line max-md:bg-surface md:grid-cols-2 lg:grid-cols-3">
        {steps.map((s) => (
          <article
            key={s.n}
            className="flex flex-col gap-2.5 max-md:grid max-md:grid-cols-[auto_1fr] max-md:gap-x-3.5 max-md:gap-y-1 max-md:p-4 md:rounded-2xl md:border md:border-line md:bg-surface md:p-6"
          >
            <span className="font-display text-xl text-accent max-md:row-span-2 md:text-[28px]">{s.n}</span>
            <h3 className="font-sans text-base font-bold md:text-[19px]">{s.title}</h3>
            <p className="text-sm leading-normal text-muted md:text-base">{s.text}</p>
          </article>
        ))}
      </div>
    </section>
  )
}
