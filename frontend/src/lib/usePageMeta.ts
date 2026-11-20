import { useEffect } from 'react'

interface PageMeta {
  title: string
  description: string
  image?: string
}

function metaTag(attr: 'name' | 'property', key: string): HTMLMetaElement {
  let tag = document.querySelector<HTMLMetaElement>(`meta[${attr}="${key}"]`)
  if (!tag) {
    tag = document.createElement('meta')
    tag.setAttribute(attr, key)
    document.head.appendChild(tag)
  }
  return tag
}

export function usePageMeta({ title, description, image }: PageMeta): void {
  useEffect(() => {
    const desc = metaTag('name', 'description')
    const ogTitle = metaTag('property', 'og:title')
    const ogDescription = metaTag('property', 'og:description')
    const ogImage = image ? metaTag('property', 'og:image') : null
    const prev = {
      title: document.title,
      desc: desc.content,
      ogTitle: ogTitle.content,
      ogDescription: ogDescription.content,
      ogImage: ogImage?.content ?? '',
    }
    document.title = title
    desc.content = description
    ogTitle.content = title
    ogDescription.content = description
    if (ogImage && image) ogImage.content = image
    return () => {
      document.title = prev.title
      desc.content = prev.desc
      ogTitle.content = prev.ogTitle
      ogDescription.content = prev.ogDescription
      if (ogImage) ogImage.content = prev.ogImage
    }
  }, [title, description, image])
}
