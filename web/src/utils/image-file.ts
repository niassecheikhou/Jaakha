// Regles communes aux images envoyees (photo de profil, piece d'identite).
// Le backend applique les memes regles : ce controle evite seulement un aller-retour inutile.
export const acceptedImageTypes = ['image/jpeg', 'image/png', 'image/webp']
export const acceptedImageAccept = 'image/png,image/jpeg,image/webp'
export const maxImageSizeInBytes = 5 * 1024 * 1024

// Retourne le message d'erreur a afficher, ou null si l'image est acceptee.
export function imageFileError(file: File) {
  if (!acceptedImageTypes.includes(file.type)) {
    return `"${file.name}" : format non accepte. Choisis une image JPG, PNG ou WEBP.`
  }
  if (file.size > maxImageSizeInBytes) {
    return `"${file.name}" est trop lourde. Choisis une image de 5 Mo maximum.`
  }
  return null
}
