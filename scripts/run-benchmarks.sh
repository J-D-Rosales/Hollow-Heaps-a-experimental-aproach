#!/usr/bin/env bash
# Campaña de benchmarks (F3/F4). Ver BENCHMARKING.md.
#
# Uso:  ./scripts/run-benchmarks.sh MODO [PARTES...]
#   MODO:   smoke  (~10-15 min) comprueba que todo corre; n = 10^4, 1 fork, iteraciones de 1 s
#           pilot  1 fork con las iteraciones del diseño; sirve para estimar la duración de full
#           full   la campaña definitiva: 3 forks, 5 warmup + 10 medición
#   PARTES: cualquier subconjunto de  main gc alloc jol nocoops perf  (por defecto, todas)
#
# Variables opcionales:
#   JAVA=/ruta/a/java     JDK 21 a usar (por defecto, el java del PATH)
#   W_TIME=2s R_TIME=2s   duración de cada iteración de warmup/medición en S1-S3 (pilot y full)
#
# Resultado: results/<modo>-<fecha>/ y un .tar.gz con todo para enviarlo de vuelta.
set -euo pipefail
cd "$(dirname "$0")/.."

MODE="${1:-smoke}"
shift || true
PARTS="${*:-main gc alloc jol nocoops perf}"
JAVA="${JAVA:-java}"
W_TIME="${W_TIME:-2s}"
R_TIME="${R_TIME:-2s}"

NS_PROP=()   # AllocProbe y JolFootprint: todos los n salvo en smoke
case "$MODE" in
  smoke) JMH_TIMING=(-f 1 -wi 1 -i 1 -w 1s -r 1s -p n=10000); S4_TIMING=("${JMH_TIMING[@]}")
         NS_PROP=(-Dbench.ns=10000) ;;
  pilot) JMH_TIMING=(-f 1 -w "$W_TIME" -r "$R_TIME");          S4_TIMING=(-f 1) ;;
  full)  JMH_TIMING=(-w "$W_TIME" -r "$R_TIME");               S4_TIMING=() ;;
  *) echo "Modo desconocido: $MODE (usa smoke, pilot o full)"; exit 1 ;;
esac

TS="$(date +%Y%m%d-%H%M%S)"
OUT="results/${MODE}-${TS}"
mkdir -p "$OUT"
exec > >(tee -a "$OUT/run.log") 2>&1

# Mismas flags que las anotaciones @Fork. Ojo: -jvmArgsAppend en la línea de comandos
# REEMPLAZA el de la anotación, así que en nocoops hay que repetirlas todas.
BASE_JVM="-Xms4g -Xmx4g -XX:+AlwaysPreTouch -XX:+UseG1GC"
JOL_FLAGS=(-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading -Djol.magicFieldOffset=true)
JAR=target/benchmarks.jar
S123="S1Insert|S2DeleteMin|S3Mixed"

log() { echo; echo "=== [$(date +%H:%M:%S)] $* ==="; }

# --- comprobaciones -----------------------------------------------------------------------
JV="$("$JAVA" -version 2>&1 | head -1)"
echo "Java: $JV"
if ! "$JAVA" -version 2>&1 | grep -qE 'version "21'; then
  echo "ERROR: se necesita JDK 21 (Temurin 21). Usa JAVA=/ruta/al/jdk-21/bin/java"; exit 1
fi
if [[ "$(uname -s)" != "Linux" ]]; then
  echo "AVISO: no es Linux; perfnorm no funcionará y los datos no valen para el paper."
fi

log "Compilando (mvn clean package, incluye los tests)"
mvn -q clean package
"$JAVA" -jar "$JAR" -l

./scripts/capture-env.sh "$OUT/environment.txt" "$JAVA"

declare -A DUR
run_part() {   # run_part nombre comando...
  local name="$1"; shift
  log "$name"
  local t0=$SECONDS
  "$@"
  DUR[$name]=$((SECONDS - t0))
  echo "--- $name: ${DUR[$name]} s"
}

jmh() { "$JAVA" -jar "$JAR" -foe true "$@"; }

for part in $PARTS; do
  case "$part" in
    main)
      run_part main-s1s2s3 jmh "$S123" "${JMH_TIMING[@]}" -rf csv -rff "$OUT/main-s1s2s3.csv"
      run_part main-s4     jmh S4DecreaseKey "${S4_TIMING[@]}" -rf csv -rff "$OUT/main-s4.csv"
      ;;
    gc)       # H3b (incluye el setup; ver alloc)
      run_part gc-s3 jmh S3Mixed "${JMH_TIMING[@]}" -prof gc -rf csv -rff "$OUT/gc-s3.csv"
      ;;
    alloc)    # H3b sin el setup
      run_part alloc "$JAVA" "${NS_PROP[@]}" $BASE_JVM -cp "$JAR" utec.hollowHeaps.bench.AllocProbe "$OUT/alloc.csv"
      ;;
    jol)      # H3a, con y sin CompressedOops
      run_part jol-coops   "$JAVA" "${NS_PROP[@]}" "${JOL_FLAGS[@]}" $BASE_JVM -cp "$JAR" \
        utec.hollowHeaps.bench.JolFootprint "$OUT/jol-coops.csv"
      run_part jol-nocoops "$JAVA" "${NS_PROP[@]}" "${JOL_FLAGS[@]}" $BASE_JVM -XX:-UseCompressedOops -cp "$JAR" \
        utec.hollowHeaps.bench.JolFootprint "$OUT/jol-nocoops.csv"
      ;;
    nocoops)  # H4b
      run_part nocoops-s3 jmh S3Mixed "${JMH_TIMING[@]}" \
        -jvmArgsAppend "$BASE_JVM -XX:-UseCompressedOops" -rf csv -rff "$OUT/nocoops-s3.csv"
      ;;
    perf)     # H4a
      if ! command -v perf >/dev/null 2>&1; then
        echo "AVISO: 'perf' no está instalado; se omite perfnorm (ver BENCHMARKING.md)"
        continue
      fi
      PARANOID="$(cat /proc/sys/kernel/perf_event_paranoid 2>/dev/null || echo 99)"
      if (( PARANOID > 1 )); then
        echo "AVISO: perf_event_paranoid=$PARANOID; se omite perfnorm."
        echo "       Ejecuta: sudo sysctl -w kernel.perf_event_paranoid=1   y repite con: $0 $MODE perf"
        continue
      fi
      run_part perfnorm-s2s3 jmh "S2DeleteMin|S3Mixed" "${JMH_TIMING[@]}" -prof perfnorm \
        -rf csv -rff "$OUT/perfnorm-s2s3.csv"
      ;;
    *) echo "Parte desconocida: $part"; exit 1 ;;
  esac
done

log "Duraciones (s)"
TOTAL=0
for k in "${!DUR[@]}"; do printf '  %-16s %6d\n' "$k" "${DUR[$k]}"; TOTAL=$((TOTAL + DUR[$k])); done
printf '  %-16s %6d  (%s h)\n' TOTAL "$TOTAL" "$(awk -v t="$TOTAL" 'BEGIN{printf "%.1f", t/3600}')"
if [[ "$MODE" == pilot ]]; then
  echo "Estimación de full: las partes JMH x3 (3 forks); alloc y jol duran lo mismo."
fi

tar -czf "${OUT}.tar.gz" -C results "$(basename "$OUT")"
log "Listo: $OUT  (enviar ${OUT}.tar.gz)"
