import { useMemo, useState } from 'react'
import type { Issue, Severity } from '../types/review'

interface DiffViewerProps {
  diff: string
  issues: Issue[]
  activeIssueId?: string | null
  onIssueClick?: (issueId: string) => void
  fileName?: string
}

interface HunkLine {
  type: 'context' | 'add' | 'remove' | 'header'
  oldLine: number | null
  newLine: number | null
  content: string
}

interface FileSection {
  fileName: string
  lines: HunkLine[]
}

const SEVERITY_DOT: Record<Severity, string> = {
  CRITICAL: 'bg-red-500 ring-red-300',
  HIGH: 'bg-orange-500 ring-orange-300',
  MEDIUM: 'bg-yellow-500 ring-yellow-300',
  LOW: 'bg-gray-400 ring-gray-300',
  INFO: 'bg-blue-400 ring-blue-300',
}

function parseDiff(diffText: string): FileSection[] {
  const sections: FileSection[] = []
  let currentSection: FileSection | null = null
  let oldLine = 0
  let newLine = 0

  for (const line of diffText.split('\n')) {
    if (line.startsWith('diff --git')) {
      if (currentSection) sections.push(currentSection)
      continue
    }
    if (line.startsWith('--- ') || line.startsWith('+++ ')) {
      if (line.startsWith('+++ b/')) {
        currentSection = { fileName: line.slice(6).trim(), lines: [] }
      }
      continue
    }
    if (line.startsWith('@@')) {
      const match = line.match(/@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/)
      if (match && currentSection) {
        oldLine = parseInt(match[1])
        newLine = parseInt(match[3])
        currentSection.lines.push({ type: 'header', oldLine: null, newLine: null, content: line })
      }
      continue
    }

    if (!currentSection) continue

    if (line.startsWith('+')) {
      currentSection.lines.push({ type: 'add', oldLine: null, newLine: newLine++, content: line.slice(1) })
    } else if (line.startsWith('-')) {
      currentSection.lines.push({ type: 'remove', oldLine: oldLine++, newLine: null, content: line.slice(1) })
    } else {
      currentSection.lines.push({ type: 'context', oldLine: oldLine++, newLine: newLine++, content: line.startsWith(' ') ? line.slice(1) : line })
    }
  }
  if (currentSection) sections.push(currentSection)
  return sections
}

export function DiffViewer({ diff, issues, activeIssueId, onIssueClick, fileName }: DiffViewerProps) {
  const sections = useMemo(() => parseDiff(diff || ''), [diff])

  const issuesByFileAndLine = useMemo(() => {
    const map = new Map<string, Issue[]>()
    for (const issue of issues) {
      const key = `${issue.file || fileName || ''}:${issue.line || 0}`
      const existing = map.get(key) || []
      existing.push(issue)
      map.set(key, existing)
    }
    return map
  }, [issues, fileName])

  const [selectedFile, setSelectedFile] = useState(0)

  if (!diff || sections.length === 0) {
    return (
      <div className="p-8 text-center text-gray-500 dark:text-gray-400 border border-dashed border-gray-300 dark:border-gray-600 rounded-xl">
        暂无 diff 数据
      </div>
    )
  }

  const section = sections[selectedFile]

  return (
    <div className="border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden bg-white dark:bg-slate-900">
      {sections.length > 1 && (
        <div className="flex border-b border-gray-200 dark:border-gray-700 overflow-x-auto bg-gray-50 dark:bg-gray-800">
          {sections.map((s, i) => (
            <button
              key={i}
              onClick={() => setSelectedFile(i)}
              className={`px-3 py-2 text-xs whitespace-nowrap border-r border-gray-200 dark:border-gray-700 transition-colors ${
                i === selectedFile
                  ? 'bg-white dark:bg-slate-900 text-emerald-700 dark:text-emerald-400 font-medium'
                  : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              {s.fileName}
            </button>
          ))}
        </div>
      )}
      <div className="overflow-x-auto font-mono text-xs leading-5">
        <table className="w-full border-collapse">
          <tbody>
            {section?.lines.map((hl, idx) => {
              const key = `${section.fileName}:${hl.newLine}`
              const lineIssues = issuesByFileAndLine.get(key) || []
              const isActive = lineIssues.some(i => i.id === activeIssueId)

              const bg = hl.type === 'add' ? 'bg-green-50 dark:bg-green-950/20'
                : hl.type === 'remove' ? 'bg-red-50 dark:bg-red-950/20'
                : hl.type === 'header' ? 'bg-blue-50 dark:bg-blue-950/20'
                : isActive ? 'bg-yellow-50 dark:bg-yellow-950/20' : ''

              return (
                <tr key={idx} className={bg}>
                  <td className="w-10 text-right pr-2 pl-2 text-gray-400 dark:text-gray-600 select-none border-r border-gray-100 dark:border-gray-800">
                    {hl.oldLine ?? ''}
                  </td>
                  <td className="w-10 text-right pr-2 text-gray-400 dark:text-gray-600 select-none border-r border-gray-100 dark:border-gray-800">
                    {hl.newLine ?? ''}
                  </td>
                  <td className="w-4 text-center relative">
                    {lineIssues.length > 0 && (
                      <span
                        onClick={() => onIssueClick?.(lineIssues[0].id)}
                        className={`inline-block w-2.5 h-2.5 rounded-full ring-1 cursor-pointer hover:scale-125 transition-transform ${
                          SEVERITY_DOT[lineIssues[0].severity] || 'bg-gray-400'
                        } ${!lineIssues[0].selected ? 'opacity-30' : ''}`}
                        title={lineIssues.map(i => i.title).join('; ')}
                      />
                    )}
                  </td>
                  <td className="pl-2 pr-4 whitespace-pre-wrap break-all">
                    <span className={
                      hl.type === 'add' ? 'text-green-700 dark:text-green-400'
                      : hl.type === 'remove' ? 'text-red-700 dark:text-red-400'
                      : hl.type === 'header' ? 'text-blue-600 dark:text-blue-400 font-semibold'
                      : 'text-gray-700 dark:text-gray-300'
                    }>
                      {hl.type === 'header' ? hl.content : hl.content}
                    </span>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
