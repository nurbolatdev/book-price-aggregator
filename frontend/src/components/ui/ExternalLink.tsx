import { forwardRef, type AnchorHTMLAttributes } from 'react'

export const ExternalLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
  function ExternalLink(props, ref) {
    return <a ref={ref} target="_blank" rel="noopener noreferrer" {...props} />
  },
)
