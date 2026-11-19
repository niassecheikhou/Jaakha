import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type ChangeEvent } from 'react'
import { accountApi, getApiError } from '../services/api'
import { acceptedImageAccept, imageFileError } from '../utils/image-file'
import { useToast } from './toast-context'

type ProfilePhotoUploaderProps = {
  currentPhoto?: string | null
  label?: string
  placeholder?: string
  onUploaded?: (photoUrl: string) => void
}

// Photo de profil (publique). La piece d'identite a son propre composant : IdentityDocumentUploader.
export function ProfilePhotoUploader({
  currentPhoto,
  label = 'Photo du profil',
  placeholder = 'Photo',
  onUploaded,
}: ProfilePhotoUploaderProps) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [photoPreview, setPhotoPreview] = useState('')
  const [uploadError, setUploadError] = useState('')
  const shownPhoto = photoPreview || currentPhoto || ''

  const uploadMutation = useMutation({
    mutationFn: accountApi.uploadPhoto,
    onSuccess: (response) => {
      setPhotoPreview(response.photoUrl)
      setUploadError('')
      // Rafraichit l'avatar, le dossier et (prestataire) le profil public.
      void queryClient.invalidateQueries({ queryKey: ['account', 'me'] })
      void queryClient.invalidateQueries({ queryKey: ['providers', 'me'] })
      onUploaded?.(response.photoUrl)
      toast.success('Image envoyee', `${label} a ete mise a jour avec succes.`)
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setPhotoPreview('')
      setUploadError(`${apiError.message} L'image n'a pas ete modifiee.`)
      toast.error('Upload impossible', apiError.message)
    },
  })

  function handlePhotoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    const error = imageFileError(file)
    if (error) {
      setPhotoPreview('')
      setUploadError(error)
      toast.error('Image refusee', error)
      event.currentTarget.value = ''
      return
    }
    setUploadError('')
    setPhotoPreview(URL.createObjectURL(file))
    uploadMutation.mutate(file)
  }

  return (
    <div className="photo-upload span-2">
      {shownPhoto ? (
        <img className="profile-photo-preview" src={shownPhoto} alt={`Apercu - ${label}`} />
      ) : (
        <div className="profile-photo-placeholder">{placeholder}</div>
      )}
      <label>
        {label}
        <input type="file" accept={acceptedImageAccept} onChange={handlePhotoChange} />
      </label>
      <span className="muted">
        {uploadMutation.isPending ? 'Upload en cours...' : 'Formats acceptes: JPG, PNG ou WEBP.'}
      </span>
      {uploadError && <span className="field-error">{uploadError}</span>}
    </div>
  )
}
