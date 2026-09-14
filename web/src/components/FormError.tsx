export function FormError({
  message,
  fields,
}: {
  message?: string
  fields?: Record<string, string>
}) {
  if (!message && (!fields || Object.keys(fields).length === 0)) {
    return null
  }

  return (
    <div className="form-error" role="alert">
      {message && <strong>{message}</strong>}
      {fields && (
        <ul>
          {Object.entries(fields).map(([field, error]) => (
            <li key={field}>{error}</li>
          ))}
        </ul>
      )}
    </div>
  )
}
