#!/usr/bin/env bash
# Guarda la información del entorno para ENVIRONMENT.md (sección 3 del diseño).
# Uso: ./scripts/capture-env.sh [archivo_salida] [java]
set -uo pipefail
OUT="${1:-results/environment.txt}"
JAVA="${2:-java}"
mkdir -p "$(dirname "$OUT")"

section() { echo; echo "## $1"; }
{
  echo "# Entorno de la corrida — $(date -Iseconds)"
  section "Sistema";         uname -a; cat /etc/os-release 2>/dev/null | grep -E '^(PRETTY_NAME|VERSION)='
  section "CPU";             lscpu 2>/dev/null || sysctl -a 2>/dev/null | grep machdep.cpu
  section "Gobernador CPU";  cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor 2>/dev/null | sort | uniq -c
  section "Turbo (intel_pstate no_turbo / boost)"
  cat /sys/devices/system/cpu/intel_pstate/no_turbo 2>/dev/null || cat /sys/devices/system/cpu/cpufreq/boost 2>/dev/null || echo "n/d"
  section "Memoria";         free -h 2>/dev/null
  section "Java";            "$JAVA" -version 2>&1
  section "Flags JVM relevantes (con -Xmx4g -XX:+UseG1GC)"
  "$JAVA" -Xmx4g -XX:+UseG1GC -XX:+PrintFlagsFinal -version 2>/dev/null \
    | grep -E ' (UseCompressedOops|UseCompressedClassPointers|UseG1GC|MaxHeapSize|ObjectAlignmentInBytes|UseCompactObjectHeaders) '
  section "Maven";           mvn -v 2>&1 | head -3
  section "perf";            perf --version 2>&1 || echo "perf no instalado"
  echo "perf_event_paranoid = $(cat /proc/sys/kernel/perf_event_paranoid 2>/dev/null || echo n/d)"
  section "Git";             git rev-parse HEAD 2>/dev/null; git status --short 2>/dev/null | head -20
  section "Carga al empezar"; uptime
} > "$OUT" 2>&1
echo "Entorno guardado en $OUT"
