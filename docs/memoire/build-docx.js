// Assemble le mémoire Word (docs/memoire/Memoire_JAANKHA.docx) à partir des chapitres Markdown.
// Usage (depuis la racine du dépôt) : node docs/memoire/build-docx.js
// Les diagrammes Mermaid sont remplacés par leurs exports PNG (docs/memoire/figures/).
const fs = require('fs')
const path = require('path')
const {
  AlignmentType, BorderStyle, Document, Footer, Header, HeadingLevel, ImageRun, LevelFormat, Packer,
  PageBreak, PageNumber, PageOrientation, Paragraph, ShadingType, Table, TableCell, TableOfContents, TableRow, TextRun,
  WidthType,
} = require('docx')

const DIR = __dirname
const FIG_DIR = path.join(DIR, 'figures')
const OUTPUT = path.join(DIR, 'Memoire_JAANKHA_Cheikhou_Niass.docx')
const FONT = 'Times New Roman'
const CONTENT_WIDTH = 9026 // A4 (11906) moins 2 marges de 1440 DXA
const BLUE = '1F3864'

// ------------------------------------------------------------------ Texte enrichi en ligne
// Gère **gras**, *italique*, `code` et [lien](url) (le lien est rendu par son texte).
function inlineRuns(text, base = {}) {
  const runs = []
  const pattern = /(\*\*[^*]+\*\*|`[^`]+`|\*[^*\s][^*]*\*|\[[^\]]+\]\([^)]+\))/g
  let last = 0
  for (const match of text.matchAll(pattern)) {
    if (match.index > last) runs.push(new TextRun({ text: text.slice(last, match.index), ...base }))
    const token = match[0]
    if (token.startsWith('**')) runs.push(new TextRun({ text: token.slice(2, -2), bold: true, ...base }))
    else if (token.startsWith('`')) runs.push(new TextRun({ text: token.slice(1, -1), font: 'Courier New', size: 20, ...base }))
    else if (token.startsWith('[')) runs.push(new TextRun({ text: token.replace(/^\[([^\]]+)\].*$/, '$1'), ...base }))
    else runs.push(new TextRun({ text: token.slice(1, -1), italics: true, ...base }))
    last = match.index + token.length
  }
  if (last < text.length) runs.push(new TextRun({ text: text.slice(last), ...base }))
  return runs
}

// Repères de type : permettent de déplacer l'introduction d'un diagramme sur sa page paysage.
const BODY_PARAGRAPHS = new WeakSet()
const SPACERS = new WeakSet()
const spacer = () => {
  const paragraph = new Paragraph({ spacing: { after: 120 }, children: [] })
  SPACERS.add(paragraph)
  return paragraph
}
const HEADING_PARAGRAPHS = new WeakSet()
const body = (text, options = {}) => {
  const paragraph = new Paragraph({
    children: inlineRuns(text), alignment: AlignmentType.JUSTIFIED, spacing: { after: 120 }, ...options,
  })
  BODY_PARAGRAPHS.add(paragraph)
  return paragraph
}
const pageBreak = () => new Paragraph({ children: [new PageBreak()] })
const caption = (text) => new Paragraph({
  children: inlineRuns(text, { italics: true, size: 21 }), alignment: AlignmentType.CENTER,
  spacing: { before: 80, after: 200 }, keepNext: text.startsWith('Tableau'),
})

// Cadre vide pour un contenu ajouté par l'auteur (captures, remerciements…).
function reservedBox(label, lines) {
  const border = { style: BorderStyle.DASHED, size: 6, color: '8EA9C1' }
  const borders = { top: border, bottom: border, left: border, right: border }
  const filler = Array.from({ length: lines }, () => new Paragraph({ children: [new TextRun('')] }))
  return new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [CONTENT_WIDTH],
    rows: [new TableRow({ cantSplit: true, children: [new TableCell({
      width: { size: CONTENT_WIDTH, type: WidthType.DXA }, borders,
      shading: { fill: 'F5F8FB', type: ShadingType.CLEAR, color: 'auto' },
      margins: { top: 120, bottom: 120, left: 160, right: 160 },
      children: [new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: label, italics: true, color: '7F8C9A', size: 20 })],
      }), ...filler],
    })] })],
  })
}

// ------------------------------------------------------------------ Images
function pngSize(file) {
  const buffer = fs.readFileSync(file)
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20), buffer }
}
// Toutes les pages sont en A4 portrait (choix de l'auteur) : aucun diagramme n'est placé en paysage.
// Pour passer un diagramme large en paysage, ajouter son numéro ici (ex. new Set([5, 6, 7])).
const LANDSCAPE_FIGURES = new Set([])

function figureImage(number, landscape = false, maxLandscapeHeight = 540) {
  const file = fs.readdirSync(FIG_DIR).find((name) => name.startsWith(`figure-${String(number).padStart(2, '0')}-`))
  if (!file) return null
  const { width, height, buffer } = pngSize(path.join(FIG_DIR, file))
  // Portrait : largeur utile ~ 600 px (16 cm), hauteur ~ 820 px. Paysage : ~ 930 px de large ; la hauteur
  // disponible diminue quand un titre et une introduction partagent la page.
  const scale = landscape ? Math.min(930 / width, maxLandscapeHeight / height, 1) : Math.min(600 / width, 820 / height, 1)
  return new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 120 }, keepNext: true,
    children: [new ImageRun({ type: 'png', data: buffer, transformation: {
      width: Math.round(width * scale), height: Math.round(height * scale) } })],
  })
}

// ------------------------------------------------------------------ Tableaux Markdown
function splitRow(line) {
  return line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((cell) => cell.trim())
}
function markdownTable(lines) {
  const header = splitRow(lines[0])
  const rows = lines.slice(2).map(splitRow)
  const count = header.length
  // Largeur des colonnes proportionnelle à la longueur moyenne du texte (bornée).
  const weights = header.map((_, i) => {
    const lengths = [header[i], ...rows.map((r) => r[i] ?? '')].map((t) => t.length)
    return Math.min(60, Math.max(8, lengths.reduce((a, b) => a + b, 0) / lengths.length))
  })
  const total = weights.reduce((a, b) => a + b, 0)
  const widths = weights.map((w) => Math.floor((w / total) * CONTENT_WIDTH))
  widths[count - 1] += CONTENT_WIDTH - widths.reduce((a, b) => a + b, 0)
  const border = { style: BorderStyle.SINGLE, size: 4, color: 'B4C2D0' }
  const borders = { top: border, bottom: border, left: border, right: border }
  // Petits tableaux (8 lignes ou moins) : gardés d'un seul bloc sur une même page.
  const keepTogether = rows.length <= 8
  const cell = (text, index, isHeader, isLastRow) => new TableCell({
    width: { size: widths[index], type: WidthType.DXA }, borders,
    shading: isHeader ? { fill: 'DCE6F0', type: ShadingType.CLEAR, color: 'auto' } : undefined,
    margins: { top: 60, bottom: 60, left: 100, right: 100 },
    children: [new Paragraph({ keepNext: keepTogether && !isLastRow,
      children: inlineRuns(text ?? '', { size: 20, bold: isHeader || undefined }) })],
  })
  return new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA }, columnWidths: widths,
    rows: [
      new TableRow({ tableHeader: true, cantSplit: true, children: header.map((h, i) => cell(h, i, true, false)) }),
      ...rows.map((r, rowIndex) => new TableRow({ cantSplit: true,
        children: header.map((_, i) => cell(r[i], i, false, rowIndex === rows.length - 1)) })),
    ],
  })
}

function codeBlock(lines) {
  return lines.map((line, index) => new Paragraph({
    shading: { fill: 'F2F4F7', type: ShadingType.CLEAR, color: 'auto' },
    spacing: { before: index === 0 ? 120 : 0, after: index === lines.length - 1 ? 160 : 0, line: 240 },
    children: [new TextRun({ text: line || ' ', font: 'Courier New', size: 17 })],
  }))
}

// ------------------------------------------------------------------ Conversion d'un fichier Markdown
const SKIPPED_NOTES = ['Mise en page', 'Maximum une page', 'Les numéros de page', 'Les diagrammes sont rédigés', 'Maximum']
const HEADINGS = { 1: HeadingLevel.HEADING_1, 2: HeadingLevel.HEADING_2, 3: HeadingLevel.HEADING_3, 4: HeadingLevel.HEADING_4 }

function convert(markdown, { pageBreakOnRule = false, startWithBreak = true } = {}) {
  const lines = markdown.replace(/\r/g, '').split('\n')
  const out = []
  let pendingFigure = null // légende de figure à placer sous l'image
  let firstHeading = true
  let breakBeforeNextHeading = false
  const flushFigure = () => { if (pendingFigure) { out.push(caption(pendingFigure)); pendingFigure = null } }

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (!line.trim()) continue

    if (line.startsWith('```')) {
      const lang = line.slice(3).trim()
      const block = []
      for (i++; i < lines.length && !lines[i].startsWith('```'); i++) block.push(lines[i])
      if (lang === 'mermaid' && pendingFigure) {
        const number = Number(pendingFigure.match(/Figure (\d+)/)[1])
        if (LANDSCAPE_FIGURES.has(number)) {
          // Marqueur traité à l'assemblage : section dédiée en orientation paysage. Le paragraphe
          // d'introduction (et son titre éventuel) l'accompagne, pour ne pas rester seul sur une page.
          const intro = []
          if (BODY_PARAGRAPHS.has(out[out.length - 1])) intro.unshift(out.pop())
          // Titres situés juste avant (jusqu'à deux niveaux, ex. « 4.2 » puis « 4.2.1 »).
          while (intro.length < 3 && HEADING_PARAGRAPHS.has(out[out.length - 1])) intro.unshift(out.pop())
          const maxHeight = 540 - intro.length * 60 // ~60 px par titre ou paragraphe d'introduction
          out.push({ landscape: [...intro, figureImage(number, true, maxHeight), caption(pendingFigure)] })
          pendingFigure = null
        } else {
          const image = figureImage(number)
          if (image) out.push(image)
          flushFigure()
        }
      } else {
        out.push(...codeBlock(block))
      }
      continue
    }
    flushFigure()

    if (line.trim() === '---') {
      // Le saut de page est porté par le titre suivant (pageBreakBefore) : jamais de page blanche.
      if (pageBreakOnRule) breakBeforeNextHeading = true
      continue
    }
    const heading = line.match(/^(#{1,4}) (.*)$/)
    if (heading) {
      const level = heading[1].length
      const pageBreakBefore = (firstHeading && startWithBreak) || breakBeforeNextHeading
      firstHeading = false
      breakBeforeNextHeading = false
      const headingParagraph = new Paragraph({ heading: HEADINGS[level], pageBreakBefore, children: inlineRuns(heading[2]) })
      HEADING_PARAGRAPHS.add(headingParagraph)
      out.push(headingParagraph)
      continue
    }
    const fig = line.match(/^\*(Figure \d+ : [^*]+)\*/)
    if (fig) { pendingFigure = fig[1]; continue }
    const tab = line.match(/^\*(Tableau \d+ : [^*]+)\*$/)
    if (tab) { out.push(caption(tab[1])); continue }

    if (line.startsWith('|')) {
      const block = []
      for (; i < lines.length && lines[i].startsWith('|'); i++) block.push(lines[i])
      i--
      out.push(markdownTable(block), spacer())
      continue
    }
    const reserved = line.match(/^\[Espace réservé : (.*)\]$/)
    if (reserved) {
      const isScreenshot = /capture/i.test(reserved[1])
      out.push(reservedBox(`Espace réservé — ${reserved[1]}`, isScreenshot ? 12 : 18),
        spacer())
      continue
    }
    if (line.startsWith('>')) {
      const note = []
      for (; i < lines.length && lines[i].startsWith('>'); i++) note.push(lines[i].replace(/^>\s?/, ''))
      i--
      const text = note.join(' ')
      if (SKIPPED_NOTES.some((s) => text.startsWith(s))) continue
      out.push(new Paragraph({ children: inlineRuns(text, { italics: true }), indent: { left: 567 },
        spacing: { after: 120 }, alignment: AlignmentType.JUSTIFIED }))
      continue
    }
    const bullet = line.match(/^(\s*)[-*] (.*)$/)
    if (bullet) {
      let text = bullet[2]
      while (i + 1 < lines.length && /^\s{2,}\S/.test(lines[i + 1]) && !/^\s*[-*] |^\s*\d+\. /.test(lines[i + 1])) text += ' ' + lines[++i].trim()
      out.push(new Paragraph({ numbering: { reference: 'bullets', level: bullet[1].length >= 2 ? 1 : 0 },
        children: inlineRuns(text), spacing: { after: 60 }, alignment: AlignmentType.JUSTIFIED }))
      continue
    }
    const numbered = line.match(/^(\d+)\. (.*)$/)
    if (numbered) {
      let text = numbered[2]
      while (i + 1 < lines.length && /^\s{2,}\S/.test(lines[i + 1])) text += ' ' + lines[++i].trim()
      out.push(new Paragraph({ children: inlineRuns(`${numbered[1]}. ${text}`), indent: { left: 567, hanging: 340 },
        spacing: { after: 60 }, alignment: AlignmentType.JUSTIFIED }))
      continue
    }
    // Paragraphe : lignes consécutives fusionnées.
    let text = line.trim()
    while (i + 1 < lines.length && lines[i + 1].trim() && !/^(#|\||>|```|\s*[-*] |\d+\. |\*(Figure|Tableau)|---|\[Espace)/.test(lines[i + 1])) {
      text += ' ' + lines[++i].trim()
    }
    out.push(body(text))
  }
  flushFigure()
  // Un espacement vide en fin de fichier peut basculer seul sur une nouvelle page (page blanche) : on le retire.
  while (SPACERS.has(out[out.length - 1])) out.pop()
  return out
}

const read = (name) => fs.readFileSync(path.join(DIR, name), 'utf8')

// ------------------------------------------------------------------ Page de garde
function coverPage() {
  const center = (runs, after = 200) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after }, children: runs })
  const logo = path.join(DIR, '..', '..', 'web', 'src', 'assets')
  const projectLogo = fs.existsSync(logo) && fs.readdirSync(logo).find((f) => /^logojaankha\.png$/i.test(f))
  const children = [
    reservedBox("Espace réservé — logo de l'Ecole 221", 4),
    center([new TextRun({ text: 'ECOLE 221', bold: true, size: 32, color: BLUE })], 120),
    center([new TextRun({ text: 'Mémoire de fin de cycle pour l’obtention de la', size: 24 })], 0),
    center([new TextRun({ text: 'Licence en Génie Logiciel', bold: true, size: 26 })], 480),
  ]
  if (projectLogo) {
    const { width, height, buffer } = pngSize(path.join(logo, projectLogo))
    const scale = Math.min(160 / width, 160 / height)
    children.push(center([new ImageRun({ type: 'png', data: buffer, transformation: {
      width: Math.round(width * scale), height: Math.round(height * scale) } })], 240))
  }
  children.push(
    center([new TextRun({ text: 'JAANKHA', bold: true, size: 56, color: BLUE })], 160),
    new Paragraph({
      alignment: AlignmentType.CENTER, spacing: { after: 720 },
      border: { bottom: { style: BorderStyle.SINGLE, size: 8, color: BLUE, space: 8 } },
      children: [new TextRun({ text: 'Conception et réalisation d’une plateforme web de mise en relation sécurisée entre employeurs et prestataires de services domestiques', size: 28, italics: true })],
    }),
    center([new TextRun({ text: 'Présenté par : ', size: 24 }), new TextRun({ text: 'Cheikhou NIASS', bold: true, size: 24 })], 160),
    center([new TextRun({ text: 'Sous l’encadrement de : ', size: 24 }), new TextRun({ text: 'M. WANE', bold: true, size: 24 })], 720),
    center([new TextRun({ text: 'Année académique 2025-2026', bold: true, size: 24 })], 0),
  )
  return children
}

// Pages liminaires : tout sauf la page de garde (construite ci-dessus).
function liminaires() {
  const text = read('00-pages-liminaires.md')
  const start = text.indexOf('## Remerciements')
  return convert(text.slice(start), { pageBreakOnRule: true, startWithBreak: true })
}

function tocBlock(title, levels) {
  return [
    new Paragraph({ heading: HeadingLevel.HEADING_1, pageBreakBefore: true, children: [new TextRun(title)] }),
    new TableOfContents(title, { hyperlink: true, headingStyleRange: levels }),
  ]
}

// ------------------------------------------------------------------ Assemblage
const chapters = ['01-introduction-generale.md', '02-chapitre-1-cadre-et-contexte.md', '03-chapitre-2-analyse-des-besoins.md',
  '04-chapitre-3-gestion-de-projet.md', '05-chapitre-4-conception.md', '06-chapitre-5-cout-du-projet.md',
  '07-chapitre-6-realisation.md', '08-chapitre-7-tests-et-validation.md', '09-chapitre-8-deploiement.md',
  '10-conclusion-generale.md']

const children = [
  ...coverPage(),
  ...liminaires(),
  ...tocBlock('Sommaire', '1-2'),
  ...chapters.flatMap((name) => convert(read(name))),
  ...tocBlock('Table des matières', '1-3'),
  ...convert(read('references.md')),
  ...convert(read('11-annexes.md')),
]

const heading = (size, before, after, color = BLUE) => ({
  run: { font: FONT, size, bold: true, color },
  paragraph: { spacing: { before, after }, keepNext: true, keepLines: true },
})

// Découpage en sections : portrait par défaut, une section paysage par diagramme large.
// L'en-tête, le pied de page et la numérotation des pages se poursuivent d'une section à l'autre.
const PORTRAIT = { width: 11906, height: 16838 }
const margin = { top: 1440, bottom: 1440, left: 1440, right: 1440 }
const header = () => new Header({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
  children: [new TextRun({ text: 'JAANKHA — Mémoire de Licence en Génie Logiciel', size: 18, color: '7F8C9A', italics: true })] })] })
const footer = () => new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
  children: [new TextRun({ children: [PageNumber.CURRENT], size: 20 })] })] })
const sections = []
let current = []
const closePortrait = (first = false) => {
  if (!current.length) return
  sections.push({
    properties: { page: { size: PORTRAIT, margin }, titlePage: first },
    headers: { default: header() }, footers: { default: footer() }, children: current,
  })
  current = []
}
for (const item of children) {
  if (item && item.landscape) {
    closePortrait(sections.length === 0)
    sections.push({
      properties: { page: { size: { ...PORTRAIT, orientation: PageOrientation.LANDSCAPE }, margin } },
      headers: { default: header() }, footers: { default: footer() },
      children: item.landscape.filter(Boolean),
    })
  } else {
    current.push(item)
  }
}
closePortrait(sections.length === 0)
// Un paragraphe "saut de page" en tête de section portrait est inutile (la section commence déjà une page).

const doc = new Document({
  creator: 'Cheikhou Niass',
  title: 'JAANKHA — Mémoire de Licence en Génie Logiciel',
  features: { updateFields: true },
  styles: {
    default: { document: { run: { font: FONT, size: 24 }, paragraph: { spacing: { line: 312 } } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...heading(34, 240, 240) },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...heading(28, 300, 160) },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...heading(25, 240, 120, '2E5C8A') },
      { id: 'Heading4', name: 'Heading 4', basedOn: 'Normal', next: 'Normal', quickFormat: true, ...heading(24, 200, 100, '2E5C8A') },
    ],
  },
  numbering: { config: [{ reference: 'bullets', levels: [
    { level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 567, hanging: 283 } } } },
    { level: 1, format: LevelFormat.BULLET, text: '◦', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 1134, hanging: 283 } } } },
  ] }] },
  sections,
})

Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(OUTPUT, buffer)
  console.log('Document généré :', OUTPUT)
})
