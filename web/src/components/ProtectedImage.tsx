import { useEffect, useState } from 'react'
import { privateFilesApi } from '../services/api'

type ProtectedImageProps = {
  src: string
  alt: string
  className?: string
  // Ouvre l'image en taille reelle dans un nouvel onglet au clic.
  zoomable?: boolean
}

// Affiche une image servie par une route authentifiee (ex: piece d'identite), qu'un <img src> ne peut pas charger seul.
export function ProtectedImage({ src, alt, className, zoomable = false }: ProtectedImageProps) {
  const [objectUrl, setObjectUrl] = useState<{ src: string; url: string }>()
  const [failedSrc, setFailedSrc] = useState<string>()

  useEffect(() => {
    let createdUrl: string | undefined
    let cancelled = false

    privateFilesApi
      .image(src)
      .then((blob) => {
        if (cancelled) {
          return
        }
        createdUrl = URL.createObjectURL(blob)
        setObjectUrl({ src, url: createdUrl })
      })
      .catch(() => {
        if (!cancelled) {
          setFailedSrc(src)
        }
      })

    return () => {
      cancelled = true
      if (createdUrl) {
        URL.revokeObjectURL(createdUrl)
      }
    }
  }, [src])

  if (failedSrc === src) {
    return <strong>Image indisponible</strong>
  }
  if (objectUrl?.src !== src) {
    return <span className="muted">Chargement...</span>
  }
  const image = <img className={className} src={objectUrl.url} alt={alt} />
  if (!zoomable) {
    return image
  }
  return (
    <a href={objectUrl.url} target="_blank" rel="noreferrer" title="Ouvrir en taille reelle">
      {image}
    </a>
  )
}
