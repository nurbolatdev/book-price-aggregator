import { bindingName } from '../../lib/format'
import type { EditionDetailResponse } from '../../api/types'

type LabelSource = Pick<EditionDetailResponse, 'publisher' | 'year' | 'binding'>

export function editionLabel({ publisher, year, binding }: LabelSource): string {
  const head = [publisher, year].filter((part) => part !== undefined && part !== '').join(' ')
  const cover = binding && binding !== 'UNKNOWN' ? bindingName(binding).toLowerCase() : ''
  return [head, cover].filter(Boolean).join(' · ')
}
