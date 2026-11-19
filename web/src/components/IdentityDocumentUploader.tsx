import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { accountApi, getApiError } from '../services/api'
import type { IdentityPhotosPayload, IdentitySide } from '../types/api'
import { acceptedImageAccept, imageFileError } from '../utils/image-file'
import { ProtectedImage } from './ProtectedImage'
import { useToast } from './toast-context'

type IdentityDocumentUploaderProps = {
  // URLs protegees deja enregistrees (null si la face n'est pas encore deposee).
  frontUrl?: string | null
  backUrl?: string | null
}

type PendingPair = { front: File; back: File }

const sideLabels: Record<IdentitySide, string> = { front: 'Recto', back: 'Verso' }
const sides: IdentitySide[] = ['front', 'back']

// Piece d'identite (CNI) : recto + verso, envoyes un par un ou ensemble.
// En selection groupee, l'ordre des fichiers renvoye par le navigateur n'est pas fiable :
// on affiche l'attribution recto/verso et on laisse l'utilisateur l'inverser avant l'envoi.
export function IdentityDocumentUploader({ frontUrl, backUrl }: IdentityDocumentUploaderProps) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [previews, setPreviews] = useState<Partial<Record<IdentitySide, string>>>({})
  const [pendingPair, setPendingPair] = useState<PendingPair | null>(null)
  const [pendingPreviews, setPendingPreviews] = useState<Record<IdentitySide, string> | null>(null)
  const [error, setError] = useState('')
  const createdUrls = useRef<string[]>([])
  const savedUrls: Record<IdentitySide, string | null | undefined> = { front: frontUrl, back: backUrl }

  // Libere les apercus locaux quand le composant disparait.
  useEffect(() => () => createdUrls.current.forEach((url) => URL.revokeObjectURL(url)), [])

  function localUrl(file: File) {
    const url = URL.createObjectURL(file)
    createdUrls.current.push(url)
    return url
  }

  const uploadMutation = useMutation({
    mutationFn: (files: IdentityPhotosPayload) => accountApi.uploadIdentityPhotos(files),
    onSuccess: (_response, files) => {
      const sent = sides.filter((side) => files[side])
      setPreviews((current) => {
        const next = { ...current }
        sent.forEach((side) => {
          const file = files[side]
          if (file) {
            next[side] = localUrl(file)
          }
        })
        return next
      })
      setPendingPair(null)
      setPendingPreviews(null)
      setError('')
      void queryClient.invalidateQueries({ queryKey: ['account', 'me'] })
      void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      const sentLabels = sent.map((side) => sideLabels[side].toLowerCase()).join(' et ')
      toast.success('Piece d\'identite envoyee', `Le ${sentLabels} a bien ete enregistre.`)
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setError(`${apiError.message} La piece d'identite n'a pas ete modifiee.`)
      toast.error('Envoi impossible', apiError.message)
    },
  })

  function rejectFiles(event: ChangeEvent<HTMLInputElement>, message: string) {
    setError(message)
    toast.error('Image refusee', message)
    event.currentTarget.value = ''
  }

  // Envoi d'une seule face, immediatement.
  function handleSideChange(side: IdentitySide, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    const fileError = imageFileError(file)
    if (fileError) {
      rejectFiles(event, fileError)
      return
    }
    setError('')
    event.currentTarget.value = ''
    uploadMutation.mutate({ [side]: file })
  }

  // Selection groupee : exactement 2 images, a confirmer avant l'envoi.
  function handlePairChange(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? [])
    if (files.length === 0) {
      return
    }
    if (files.length !== 2) {
      rejectFiles(event, files.length === 1
        ? 'Selectionne 2 images (recto et verso), ou utilise le bouton de la face a remplacer.'
        : 'Selectionne seulement 2 images : le recto et le verso.')
      return
    }
    const fileError = files.map(imageFileError).find(Boolean)
    if (fileError) {
      rejectFiles(event, fileError)
      return
    }
    setError('')
    event.currentTarget.value = ''
    const pair = { front: files[0], back: files[1] }
    setPendingPair(pair)
    setPendingPreviews({ front: localUrl(pair.front), back: localUrl(pair.back) })
  }

  function swapPendingPair() {
    if (!pendingPair || !pendingPreviews) {
      return
    }
    setPendingPair({ front: pendingPair.back, back: pendingPair.front })
    setPendingPreviews({ front: pendingPreviews.back, back: pendingPreviews.front })
  }

  function cancelPendingPair() {
    setPendingPair(null)
    setPendingPreviews(null)
  }

  const missingSides = sides.filter((side) => !previews[side] && !savedUrls[side])
  const busy = uploadMutation.isPending

  return (
    <div className="identity-upload span-2">
      <div className="identity-upload-header">
        <strong>Piece d'identite (CNI) : recto et verso</strong>
        <span className={missingSides.length === 0 ? 'identity-status complete' : 'identity-status'}>
          {missingSides.length === 0
            ? 'Recto et verso deposes'
            : `A deposer : ${missingSides.map((side) => sideLabels[side].toLowerCase()).join(' et ')}`}
        </span>
      </div>

      {pendingPair && pendingPreviews ? (
        <div className="identity-pending">
          <p className="muted">Verifie l'attribution avant l'envoi. Si les faces sont inversees, clique sur « Inverser ».</p>
          <div className="identity-sides">
            {sides.map((side) => (
              <figure className="identity-side" key={side}>
                <figcaption>{sideLabels[side]}</figcaption>
                <img className="identity-preview" src={pendingPreviews[side]} alt={`${sideLabels[side]} selectionne : ${pendingPair[side].name}`} />
                <span className="muted identity-filename">{pendingPair[side].name}</span>
              </figure>
            ))}
          </div>
          <div className="identity-actions">
            <button className="button secondary" type="button" onClick={swapPendingPair} disabled={busy}>
              Inverser recto / verso
            </button>
            <button className="button primary" type="button" onClick={() => uploadMutation.mutate(pendingPair)} disabled={busy}>
              {busy ? 'Envoi en cours...' : 'Envoyer recto et verso'}
            </button>
            <button className="button ghost" type="button" onClick={cancelPendingPair} disabled={busy}>
              Annuler
            </button>
          </div>
        </div>
      ) : (
        <>
          <div className="identity-sides">
            {sides.map((side) => (
              <figure className="identity-side" key={side}>
                <figcaption>{sideLabels[side]}</figcaption>
                {previews[side] ? (
                  <img className="identity-preview" src={previews[side]} alt={`${sideLabels[side]} de la piece d'identite`} />
                ) : savedUrls[side] ? (
                  <ProtectedImage className="identity-preview" src={savedUrls[side] ?? ''} alt={`${sideLabels[side]} de la piece d'identite`} />
                ) : (
                  <div className="identity-preview identity-placeholder">{sideLabels[side]}</div>
                )}
                <label className="identity-file-button">
                  {previews[side] || savedUrls[side] ? `Remplacer le ${sideLabels[side].toLowerCase()}` : `Ajouter le ${sideLabels[side].toLowerCase()}`}
                  <input
                    type="file"
                    accept={acceptedImageAccept}
                    disabled={busy}
                    onChange={(event) => handleSideChange(side, event)}
                  />
                </label>
              </figure>
            ))}
          </div>
          <label className="identity-pair-button">
            Ou selectionner le recto et le verso ensemble (2 images)
            <input type="file" accept={acceptedImageAccept} multiple disabled={busy} onChange={handlePairChange} />
          </label>
        </>
      )}

      <span className="muted">
        {busy ? 'Envoi en cours...' : 'Formats acceptes : JPG, PNG ou WEBP, 5 Mo maximum par image. Document prive, visible uniquement par toi et l\'administration.'}
      </span>
      {error && <span className="field-error">{error}</span>}
    </div>
  )
}
