import { useMutation } from '@tanstack/react-query'
import { useState, type ChangeEvent } from 'react'
import { accountApi, getApiError } from '../services/api'
import { useToast } from './toast-context'

const acceptedPhotoTypes = ['image/jpeg', 'image/png', 'image/webp']
const maxPhotoSizeInBytes = 5 * 1024 * 1024

type ProfilePhotoUploaderProps = {
  currentPhoto?: string | null
  label?: string
  placeholder?: string
  uploadKind?: 'profile' | 'identity'
  onUploaded?: (photoUrl: string) => void
}

export function ProfilePhotoUploader({
  currentPhoto,
  label = 'Photo du profil',
  placeholder = 'Photo',
  uploadKind = 'profile',
  onUploaded,
}: ProfilePhotoUploaderProps) {
  const toast = useToast()
  const [photoPreview, setPhotoPreview] = useState('')
  const [uploadError, setUploadError] = useState('')
  const shownPhoto = photoPreview || currentPhoto || ''

  const uploadMutation = useMutation({
    mutationFn: uploadKind === 'identity' ? accountApi.uploadIdentityPhoto : accountApi.uploadPhoto,
    onSuccess: (response) => {
      setPhotoPreview(response.photoUrl)
      setUploadError('')
      onUploaded?.(response.photoUrl)
      toast.success('Image envoyee', `${label} a ete mise a jour avec succes.`)
    },
    onError: (caughtError) => {
      const apiError = getApiError(caughtError)
      setUploadError(`${apiError.message} L'image n'a pas ete modifiee.`)
      toast.error('Upload impossible', apiError.message)
    },
  })

  function handlePhotoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    if (!acceptedPhotoTypes.includes(file.type)) {
      const message = 'Format non accepte. Choisis une image au format JPG, PNG ou WEBP.'
      setPhotoPreview('')
      setUploadError(message)
      toast.error('Image refusee', message)
      event.currentTarget.value = ''
      return
    }
    if (file.size > maxPhotoSizeInBytes) {
      const message = "L'image est trop lourde. Choisis une image de 5 Mo maximum."
      setPhotoPreview('')
      setUploadError(message)
      toast.error('Image refusee', message)
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
        <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handlePhotoChange} />
      </label>
      <span className="muted">
        {uploadMutation.isPending ? 'Upload en cours...' : 'Formats acceptes: JPG, PNG ou WEBP.'}
      </span>
      {uploadError && <span className="field-error">{uploadError}</span>}
    </div>
  )
}
