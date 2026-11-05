# Cómo correr los benchmarks (Linux)

Este documento explica cómo ejecutar la campaña de medición del estudio (Hollow Heap,
Fibonacci Heap, Indexed Binary Heap y `java.util.PriorityQueue` en Java 21) en una
máquina Linux y cómo devolver los resultados. No hace falta saber nada de las estructuras:
todo se lanza con un script.

**Resumen:** instalar JDK 21 + Maven + perf → `smoke` (15 min) → `pilot` (unas horas) →
`full` (dejarlo corriendo de noche) → enviar el `.tar.gz` de `results/`.

---

## 1. Requisitos

| Qué | Versión | Comprobar con |
|---|---|---|
| Linux **nativo** (no WSL, no máquina virtual) | cualquiera reciente | `uname -a` |
| Eclipse Temurin JDK | **21** | `java -version` → `openjdk version "21.0.x" ... Temurin` |
| Maven | ≥ 3.9 | `mvn -v` (debe decir `Java version: 21`) |
| perf | el de tu kernel | `perf --version` |
| RAM | ≥ 8 GB (los benchmarks usan un heap de 4 GB) | `free -h` |

### Instalar Temurin 21 (Ubuntu/Debian)

```bash
sudo apt install -y wget apt-transport-https gpg
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
echo "deb https://packages.adoptium.net/artifactory/deb $(. /etc/os-release; echo $VERSION_CODENAME) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update && sudo apt install -y temurin-21-jdk maven
```

Otra opción, sin `sudo`: [SDKMAN](https://sdkman.io) → `sdk list java | grep "21.*tem"`,
luego `sdk install java <la versión 21.x-tem que salga>` y `sdk install maven`.

Si tienes varios JDK, elige el 21 con `sudo update-alternatives --config java`, o pásalo
al script con `JAVA=/usr/lib/jvm/temurin-21-jdk-amd64/bin/java`.

### Instalar y habilitar perf (para los contadores de caché)

```bash
sudo apt install -y linux-tools-common linux-tools-$(uname -r)    # Ubuntu
sudo sysctl -w kernel.perf_event_paranoid=1                        # se pierde al reiniciar
perf stat -e cycles,cache-misses true                              # debe imprimir números, no "<not supported>"
```

Si `perf` no funciona, el script se salta esa parte y sigue con el resto; avisa en ese caso.

---

## 2. Preparar la máquina (importante para que los datos valgan)

Durante `pilot` y `full`:

- **Enchufada a la corriente** (si es portátil) y sin modo ahorro de energía.
- **No usar la máquina**: cerrar navegador, IDE, Discord, Spotify, etc. Nada de compilar
  otras cosas en paralelo.
- Si se puede, gobernador de CPU en `performance`:
  ```bash
  sudo apt install -y linux-tools-common   # trae cpupower
  sudo cpupower frequency-set -g performance
  ```
- Usar `tmux` o `nohup` para que la corrida no muera si se cierra la terminal o la sesión SSH.

El script guarda el estado de la máquina (CPU, gobernador, memoria, versión de Java, etc.)
en `environment.txt` dentro de los resultados; no hace falta anotarlo a mano.

---

## 3. Ejecutar

```bash
git clone <URL del repo> && cd Hollow-Heaps-a-experimental-aproach
chmod +x scripts/*.sh
```

### Paso A: `smoke` (~15 min) — ¿funciona todo?

```bash
./scripts/run-benchmarks.sh smoke
```

Compila, corre los tests y ejecuta todo con n = 10⁴ y iteraciones de 1 s. Los números
**no sirven** para el paper; solo comprueba que nada falla. Debe terminar con
`Listo: results/smoke-...`. Si algo falla, envía `results/smoke-*/run.log`.

### Paso B: `pilot` — ¿cuánto va a tardar?

```bash
tmux new -s bench
./scripts/run-benchmarks.sh pilot
```

Igual que la campaña definitiva pero con 1 fork en vez de 3. Al final imprime cuánto tardó
cada parte. **Envía ese resumen antes de lanzar `full`**: con él decidimos si hay que
recortar algo. Estimación de `full` ≈ (partes JMH del piloto) × 3 + alloc + jol.

### Paso C: `full` — la campaña definitiva

```bash
tmux new -s bench
./scripts/run-benchmarks.sh full
# Ctrl+B y luego D para salir de tmux sin pararlo; tmux attach -t bench para volver
```

Se puede correr por partes si no cabe en una noche (cada llamada crea su propia carpeta):

```bash
./scripts/run-benchmarks.sh full main            # lo principal (tiempos S1–S4)
./scripts/run-benchmarks.sh full gc alloc jol    # memoria
./scripts/run-benchmarks.sh full nocoops perf    # caché y punteros
```

---

## 4. Qué devolver

Cada corrida deja una carpeta `results/<modo>-<fecha>/` y un archivo
`results/<modo>-<fecha>.tar.gz`. **Envía los `.tar.gz`** (de `pilot` y de `full`).

| Archivo | Qué mide | Hipótesis |
|---|---|---|
| `main-s1s2s3.csv` | Tiempo de S1 (insert), S2 (deleteMin), S3 (mezcla con k) | RQ1, H2a, H2b |
| `main-s4.csv` | Latencia de `decreaseKey` aislado: mediana, p99 | H1 |
| `gc-s3.csv` | S3 con `-prof gc` (asignación y GC) | H3b |
| `alloc.csv` | B/op de S3 **sin** el setup | H3b |
| `jol-coops.csv`, `jol-nocoops.csv` (+ `-footprint.txt`) | Bytes por elemento según k | H3a |
| `nocoops-s3.csv` | S3 con `-XX:-UseCompressedOops` | H4b |
| `perfnorm-s2s3.csv` | Fallos L1/LLC y ciclos por operación | H4a |
| `environment.txt` | Hardware, SO, JVM y flags | ENVIRONMENT.md |
| `run.log` | Todo lo que se imprimió | — |

---

## 5. Problemas comunes

| Síntoma | Solución |
|---|---|
| `ERROR: se necesita JDK 21` | `JAVA=/ruta/al/jdk-21/bin/java ./scripts/run-benchmarks.sh ...` y revisa que `mvn -v` diga Java 21 |
| `perf_event_paranoid=... se omite perfnorm` | `sudo sysctl -w kernel.perf_event_paranoid=1` y luego `./scripts/run-benchmarks.sh full perf` |
| `OutOfMemoryError` | La máquina tiene poca RAM libre; cierra programas. Avisa si persiste |
| `bad interpreter: /usr/bin/env: bash^M` | Finales de línea de Windows: `sed -i 's/\r$//' scripts/*.sh` |
| La corrida se cortó a medias | Repite solo las partes que faltan (ver Paso C) |

---

## Anexo: qué hay en el código (para el equipo)

Todo está en `src/main/java/utec/hollowHeaps/bench/`:

| Clase | Escenario |
|---|---|
| `S1Insert` | S1: n `insert` con claves aleatorias |
| `S2DeleteMin` | S2: heap con n elementos que se vacía con `deleteMin` |
| `S3Mixed` + `S3Workload` | S3: rondas de k `decreaseKey` + 1 `deleteMin` + 1 `insert`, k ∈ {0, 1, 4, 16, 64} |
| `S4DecreaseKey` | S4: `decreaseKey` aislado en modo `SampleTime` |
| `JolFootprint` | Bytes por elemento (JOL) en S1 y al final de S3 |
| `AllocProbe` | B/op de S3 contando solo la parte medida |
| `Heaps` | Fábrica `HH`/`FH`/`IBH`/`PQL` |

**Cómo leer los números de JMH:** en S1, S2 y S3, `Score` es el tiempo de **una invocación
completa**, no de una operación. ns/op = Score / n en S1 y S2, y
Score / (ROUNDS · (k + 2)) en S3, con ROUNDS = 100 000. En S4 el Score ya es por operación.

Correr un benchmark suelto a mano:

```bash
mvn clean package
java -jar target/benchmarks.jar S3Mixed -f 1 -wi 1 -i 2 -p impl=HH,IBH -p n=10000 -p k=4
```
