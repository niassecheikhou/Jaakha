// Faces de la piece d'identite (CNI).
export type IdentitySide = 'front' | 'back'

// Fichiers a envoyer : une face ou les deux.
export type IdentityPhotosPayload = Partial<Record<IdentitySide, File>>

// URLs protegees renvoyees par le backend apres l'envoi.
export type IdentityPhotosResponse = {
  identityPhotoUrl: string | null
  identityPhotoBackUrl: string | null
}
