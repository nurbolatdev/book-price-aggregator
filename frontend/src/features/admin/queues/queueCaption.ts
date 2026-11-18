export function queueCaption(name: string | undefined): string | undefined {
  if (!name) return undefined;
  if (name.startsWith("kr.parse.")) return "задачи на парсинг";
  switch (name) {
    case "kr.price-changed.beacons":
      return "изменения цены → маячки";
    case "kr.price-changed.cache":
      return "изменения цены → кэш";
    case "kr.notify.email":
      return "письма к отправке";
    case "kr.dead-letter":
      return "сообщения с ошибкой";
    default:
      return undefined;
  }
}

export const DEAD_LETTER = "kr.dead-letter";
