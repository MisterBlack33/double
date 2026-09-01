<#
Find-DeadCode.ps1

Durchsucht ein Java-Projekt (src/, test/) heuristisch nach ungenutzten
Klassen und Methoden. Kein echter Parser — regex-basiert, daher als
Kandidatenliste zu verstehen, nicht als 100% verlässliches Ergebnis.

Vorgehen:
  1. Alle .java-Dateien einlesen.
  2. Klassen-/Methodennamen per Regex extrahieren (inkl. Deklarationszeile).
  3. Für jeden Namen den gesamten Projektquelltext nach Vorkommen durchsuchen,
     abzüglich der eigenen Deklarationszeile.
  4. Namen mit 0 weiteren Fundstellen = Kandidat für Dead Code.

Ausnahmen (werden übersprungen, da typischerweise per Reflection/Framework
oder als Einstiegspunkt genutzt):
  - main(String[] args)
  - Konstruktoren
  - Getter/Setter aus Records (kompakte Konstruktoren)
  - Standard-Overrides: toString, equals, hashCode, main

Nutzung:
  .\Find-DeadCode.ps1 -RootPath "C:\Pfad\zum\Projekt"
  .\Find-DeadCode.ps1                # nutzt aktuelles Verzeichnis
  .\Find-DeadCode.ps1 -OutFile report.csv
#>

param(
    [string]$RootPath = "C:\Users\elino\IdeaProjects\nettool",
    [string]$OutFile  = "dead_code_report.csv"
)

$ErrorActionPreference = "Stop"

# ── Sammlung aller Java-Dateien ─────────────────────────────────────────────

$javaFiles = Get-ChildItem -Path $RootPath -Recurse -Filter *.java -File |
        Where-Object { $_.FullName -notmatch '[\\/](target|build|out)[\\/]' }

if ($javaFiles.Count -eq 0) {
    Write-Host "Keine .java-Dateien unter '$RootPath' gefunden." -ForegroundColor Yellow
    exit 0
}

Write-Host "Lese $($javaFiles.Count) Java-Dateien ein..." -ForegroundColor Cyan

# Datei-Inhalte cachen (für spätere Volltextsuche)
$fileContents = @{}
foreach ($f in $javaFiles) {
    $fileContents[$f.FullName] = Get-Content -Raw -LiteralPath $f.FullName
}

$allSourceConcat = [string]::Join("`n", $fileContents.Values)

# ── Regex-Definitionen ───────────────────────────────────────────────────────

# Klassen/Interfaces/Enums/Records: optionale Modifier, dann class/interface/enum/record NAME
$classPattern = [regex]'(?m)^\s*(?:public|private|protected|final|abstract|static|sealed|non-sealed|\s)*\b(?:class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)'

# Methoden: Modifier* Rückgabetyp Name(...)  – bewusst grob gehalten
$methodPattern = [regex]'(?m)^\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:public|private|protected|static|final|synchronized|abstract|native|default|\s)+[\w\<\>\[\],\.\s]+?\s+([A-Za-z_][A-Za-z0-9_]*)\s*\([^;{]*\)\s*(?:throws\s+[\w,\s]+)?\s*[{;]'

$excludedMethodNames = @(
    'main','toString','equals','hashCode','clone','run',
    'get','set' # zu generisch, oft dynamisch/reflektiv genutzt
)

# ── Sammelstrukturen ─────────────────────────────────────────────────────────

$classResults  = New-Object System.Collections.Generic.List[object]
$methodResults = New-Object System.Collections.Generic.List[object]

function Count-Occurrences {
    param([string]$text, [string]$needle)
    if ([string]::IsNullOrWhiteSpace($needle)) { return 0 }
    $esc = [regex]::Escape($needle)
    $pattern = "\b$esc\b"
    return ([regex]::Matches($text, $pattern)).Count
}

# ── Klassen prüfen ────────────────────────────────────────────────────────────

Write-Host "Analysiere Klassen..." -ForegroundColor Cyan

foreach ($f in $javaFiles) {
    $content = $fileContents[$f.FullName]
    foreach ($m in $classPattern.Matches($content)) {
        $className = $m.Groups[1].Value

        # main-Klasse und Test-Klassen (JUnit findet sie über Annotationen/Runner) ausschließen
        if ($className -eq 'Main') { continue }

        $totalCount = Count-Occurrences -text $allSourceConcat -needle $className
        # 1 Treffer = nur die eigene Deklaration
        if ($totalCount -le 1) {
            $classResults.Add([PSCustomObject]@{
                Type   = 'Class'
                Name   = $className
                File   = $f.FullName.Substring($RootPath.Length).TrimStart('\','/')
                Hits   = $totalCount
            })
        }
    }
}

# ── Methoden prüfen ───────────────────────────────────────────────────────────

Write-Host "Analysiere Methoden..." -ForegroundColor Cyan

foreach ($f in $javaFiles) {
    $content = $fileContents[$f.FullName]

    # Enthaltende Klasse ermitteln (letzte class/interface/enum/record vor der Methode)
    $classMatches = $classPattern.Matches($content)

    foreach ($m in $methodPattern.Matches($content)) {
        $methodName = $m.Groups[1].Value

        if ($excludedMethodNames -contains $methodName) { continue }
        if ($methodName -match '^(if|for|while|switch|catch|synchronized)$') { continue }

        # zugehörige Klasse bestimmen (die letzte Klassendeklaration vor dieser Methode)
        $enclosingClass = "?"
        foreach ($cm in $classMatches) {
            if ($cm.Index -lt $m.Index) { $enclosingClass = $cm.Groups[1].Value }
            else { break }
        }

        $totalCount = Count-Occurrences -text $allSourceConcat -needle $methodName
        if ($totalCount -le 1) {
            $methodResults.Add([PSCustomObject]@{
            Type   = 'Method'
            Class  = $enclosingClass
            Name   = $methodName
            File   = $f.FullName.Substring($RootPath.Length).TrimStart('\','/')
            Hits   = $totalCount
            })
        }
    }
}

# ── Ausgabe ───────────────────────────────────────────────────────────────────

Write-Host "`n=== Ungenutzte Klassen ($($classResults.Count)) ===" -ForegroundColor Yellow
$classResults | Sort-Object File, Name | Format-Table Name, File -AutoSize

Write-Host "`n=== Ungenutzte Methoden ($($methodResults.Count)) ===" -ForegroundColor Yellow
$methodResults | Sort-Object Class, Name | Format-Table Class, Name, File -AutoSize

$all = @()
$all += $classResults
$all += $methodResults
$all | Export-Csv -Path $OutFile -NoTypeInformation -Encoding UTF8

Write-Host "`nBericht gespeichert unter: $OutFile" -ForegroundColor Green
Write-Host "Hinweis: Regex-basiert -> false positives moeglich (Reflection, JUnit-Annotationen, Interface-Implementierungen ueber verschiedene Namen)." -ForegroundColor DarkYellow