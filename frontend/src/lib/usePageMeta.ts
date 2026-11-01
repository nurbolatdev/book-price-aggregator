import { useEffect } from 'react'

interface PageMeta {
  title: string
  description: string
}

function descriptionTag(): HTMLMetaElement {
  let tag = document.querySelector<HTMLMetaElement>('meta[name="description"]')
  if (!tag) {
    tag = document.createElement('meta')
    tag.name = 'description'
    document.head.appendChild(tag)
  }
  return tag
}

export function usePageMeta({ title, description }: PageMeta): void {
  useEffect(() => {
    const tag = descriptionTag()
    const prevTitle = document.title
    const prevDescription = tag.content
    document.title = title
    tag.content = description
    return () => {
      document.title = prevTitle
      tag.content = prevDescription
    }
  }, [title, description])
}
