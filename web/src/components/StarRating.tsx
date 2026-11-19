type StarRatingProps = {
  rating: number
  max?: number
}

export function StarRating({ rating, max = 5 }: StarRatingProps) {
  const normalizedRating = Number.isFinite(rating) ? Math.max(0, Math.min(max, rating)) : 0
  const roundedRating = Math.round(normalizedRating)

  return (
    <span className="star-rating" aria-label={`Note ${normalizedRating.toFixed(1)} sur ${max}`} title={`Note ${normalizedRating.toFixed(1)} sur ${max}`}>
      {Array.from({ length: max }, (_, index) => {
        const filled = index < roundedRating

        return (
          <span className={`star-rating-star ${filled ? 'filled' : ''}`} aria-hidden="true" key={index}>
            ★
          </span>
        )
      })}
    </span>
  )
}
