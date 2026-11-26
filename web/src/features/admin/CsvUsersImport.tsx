import { useState, type FormEvent } from 'react'
import { useToast } from '../../components/toast-context'
import { getApiError } from '../../services/api'
import type { AdminCreateUserPayload, User, UserRole } from '../../types/api'

type CsvImportFailure = {
  message: string
  row: number
}

const csvColumnAliases = {
  fullName: ['nomcomplet', 'nom', 'fullname', 'name'],
  email: ['email', 'emailaddress', 'mail', 'courriel'],
  phone: ['telephone', 'phone', 'tel', 'numero', 'numerotelephone'],
  password: ['motdepasse', 'motdepassepardefaut', 'modepassepardefaut', 'password', 'defaultpassword'],
  role: ['role', 'profil', 'typecompte'],
}

function normalizeCsvValue(value: string) {
  return value
    .trim()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '')
}

function detectCsvDelimiter(firstLine: string) {
  const semicolonCount = (firstLine.match(/;/g) ?? []).length
  const commaCount = (firstLine.match(/,/g) ?? []).length
  return semicolonCount > commaCount ? ';' : ','
}

function parseCsvRows(csvText: string) {
  const firstLine = csvText.split(/\r?\n/).find((line) => line.trim().length > 0) ?? ''
  const delimiter = detectCsvDelimiter(firstLine)
  const rows: string[][] = []
  let row: string[] = []
  let cell = ''
  let inQuotes = false

  for (let index = 0; index < csvText.length; index += 1) {
    const char = csvText[index]
    const nextChar = csvText[index + 1]

    if (char === '"' && inQuotes && nextChar === '"') {
      cell += '"'
      index += 1
      continue
    }

    if (char === '"') {
      inQuotes = !inQuotes
      continue
    }

    if (char === delimiter && !inQuotes) {
      row.push(cell.trim())
      cell = ''
      continue
    }

    if ((char === '\n' || char === '\r') && !inQuotes) {
      if (char === '\r' && nextChar === '\n') {
        index += 1
      }
      row.push(cell.trim())
      if (row.some((value) => value.length > 0)) {
        rows.push(row)
      }
      row = []
      cell = ''
      continue
    }

    cell += char
  }

  row.push(cell.trim())
  if (row.some((value) => value.length > 0)) {
    rows.push(row)
  }

  return rows
}

function findCsvColumn(headers: string[], aliases: string[]) {
  return headers.findIndex((header) => aliases.includes(normalizeCsvValue(header)))
}

function normalizeImportedRole(value: string): UserRole | null {
  const normalizedRole = normalizeCsvValue(value)

  if (normalizedRole === 'prestataire' || normalizedRole === 'provider') {
    return 'PRESTATAIRE'
  }
  if (normalizedRole === 'employeur' || normalizedRole === 'employer') {
    return 'EMPLOYEUR'
  }
  return null
}

function validateImportedUser(payload: {
  email: string
  fullName: string
  password: string
  phone: string
  role: UserRole | null
}) {
  const errors: string[] = []

  if (payload.fullName.length < 5) {
    errors.push('nom complet trop court (5 caracteres minimum)')
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) {
    errors.push('email invalide')
  }
  if (!/^\+221(70|71|76|77|78)[0-9]{7}$/.test(payload.phone)) {
    errors.push('telephone invalide (format attendu: +22177XXXXXXX)')
  }
  if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{6,}$/.test(payload.password)) {
    errors.push('mot de passe trop faible (majuscule, minuscule, chiffre, caractere special, 6 caracteres minimum)')
  }
  if (!payload.role) {
    errors.push('role invalide (utilise employeur ou prestataire)')
  }

  return errors
}

function formatCsvApiError(error: unknown) {
  const apiError = getApiError(error)
  const fieldLabels: Record<string, string> = {
    email: 'email',
    fullName: 'nom complet',
    password: 'mot de passe',
    phone: 'telephone',
    role: 'role',
  }
  const fieldMessages = Object.entries(apiError.fields ?? {})
    .map(([field, message]) => `${fieldLabels[field] ?? field}: ${message}`)

  return fieldMessages.length > 0 ? fieldMessages.join('; ') : apiError.message
}

function accountCountLabel(count: number) {
  return `${count} compte${count > 1 ? 's' : ''}`
}

function rowCountLabel(count: number) {
  return `${count} ligne${count > 1 ? 's' : ''}`
}

function csvImportSummary(created: number, failures: CsvImportFailure[]) {
  if (created > 0 && failures.length === 0) {
    return `Import termine : ${accountCountLabel(created)} cree${created > 1 ? 's' : ''} avec succes.`
  }
  if (created > 0) {
    return `Import termine avec erreurs : ${accountCountLabel(created)} cree${created > 1 ? 's' : ''}, ${rowCountLabel(failures.length)} a verifier.`
  }
  return `Aucun compte cree. ${rowCountLabel(failures.length)} a verifier.`
}

export function CsvUsersImport({
  loading,
  onCreate,
}: {
  loading: boolean
  onCreate: (payload: AdminCreateUserPayload) => Promise<User>
}) {
  const [file, setFile] = useState<File | null>(null)
  const [fileInputKey, setFileInputKey] = useState(0)
  const [importing, setImporting] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState<{ created: number; failures: CsvImportFailure[] } | null>(null)
  const toast = useToast()
  const importInProgress = importing && !result

  async function handleImport(event: FormEvent) {
    event.preventDefault()
    setError('')
    setResult(null)

    if (!file) {
      setError('Choisis un fichier CSV a importer.')
      return
    }

    setImporting(true)
    const failures: CsvImportFailure[] = []
    let created = 0

    try {
      const rows = parseCsvRows(await file.text())
      if (rows.length < 2) {
        setError('Le fichier CSV doit contenir une ligne d en-tete et au moins un utilisateur.')
        return
      }

      const headers = rows[0]
      const columnIndexes = {
        fullName: findCsvColumn(headers, csvColumnAliases.fullName),
        email: findCsvColumn(headers, csvColumnAliases.email),
        phone: findCsvColumn(headers, csvColumnAliases.phone),
        password: findCsvColumn(headers, csvColumnAliases.password),
        role: findCsvColumn(headers, csvColumnAliases.role),
      }
      const missingColumns = Object.entries(columnIndexes)
        .filter(([, columnIndex]) => columnIndex < 0)
        .map(([column]) => column)

      if (missingColumns.length > 0) {
        setError('Colonnes attendues: nom complet, email, telephone, mot de passe par defaut, role.')
        return
      }

      for (const [index, row] of rows.slice(1).entries()) {
        const rowNumber = index + 2
        const fullName = row[columnIndexes.fullName]?.trim() ?? ''
        const email = row[columnIndexes.email]?.trim() ?? ''
        const phone = row[columnIndexes.phone]?.trim() ?? ''
        const password = row[columnIndexes.password]?.trim() ?? ''
        const role = normalizeImportedRole(row[columnIndexes.role] ?? '')
        const validationErrors = validateImportedUser({ email, fullName, password, phone, role })

        if (validationErrors.length > 0 || !role) {
          failures.push({
            row: rowNumber,
            message: validationErrors.join('; '),
          })
          continue
        }

        try {
          await onCreate({ fullName, email, phone, password, role })
          created += 1
        } catch (caughtError) {
          failures.push({
            row: rowNumber,
            message: formatCsvApiError(caughtError),
          })
        }
      }

      setImporting(false)
      setResult({ created, failures })
      if (failures.length > 0) {
        toast.error('Import CSV a verifier', csvImportSummary(created, failures))
      } else if (created > 0) {
        setFile(null)
        setFileInputKey((current) => current + 1)
        toast.success('Import CSV termine', csvImportSummary(created, failures))
      }
    } catch {
      setError('Impossible de lire ce fichier CSV.')
    } finally {
      setImporting(false)
    }
  }

  return (
    <section className="admin-import-panel" aria-labelledby="csv-import-title">
      <div>
        <h3 id="csv-import-title">Importer des comptes CSV</h3>
        <p>
          Colonnes: nom complet, email, telephone, mot de passe par defaut, role.
        </p>
        <a className="csv-template-link" href="/modele-import-comptes.csv" download>
          Telecharger le modele CSV
        </a>
      </div>
      <form className="csv-import-form" onSubmit={handleImport}>
        <label>
          Fichier CSV
          <input
            accept=".csv,text/csv"
            key={fileInputKey}
            type="file"
            onChange={(event) => {
              setFile(event.target.files?.[0] ?? null)
              setError('')
              setResult(null)
            }}
          />
        </label>
        <button className="button secondary" disabled={loading || importInProgress} type="submit">
          {importInProgress ? 'Import en cours...' : 'Importer le CSV'}
        </button>
      </form>
      {error && <p className="field-error">{error}</p>}
      {result && (
        <div className="csv-import-result">
          <strong>{csvImportSummary(result.created, result.failures)}</strong>
          {result.failures.length > 0 && (
            <ul>
              {result.failures.slice(0, 6).map((failure) => (
                <li key={`${failure.row}-${failure.message}`}>
                  Ligne {failure.row}: {failure.message}
                </li>
              ))}
            </ul>
          )}
          {result.failures.length > 6 && <span>{result.failures.length - 6} autre(s) erreur(s).</span>}
        </div>
      )}
    </section>
  )
}
