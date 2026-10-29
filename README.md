# Documento de Diseño del Estudio v1.2

Oct 29, 2026 · @Daniel

**Título de trabajo:** Evaluating Pointer-Based vs. Array-Based Priority Queues in Modern Java: A Comparative Study of Hollow Heap, Fibonacci Heap, and Binary Heap in Java 21

**Estado:** BORRADOR PARA CONGELAR. La Fase 1 está en curso: las cuatro estructuras ya implementan la API primitiva `VertexHeap` y están en auditoría. Tras el congelamiento, el documento solo se modifica mediante enmiendas registradas en la sección 13.

**Cambios respecto a v1.1:** el diseño aún no está congelado, así que los cambios de la Fase 1 (API primitiva, contrato común, H3a reformulada) se registran como versión v1.2 en la sección 12 y no como enmiendas. El detalle está en la sección 12 y en el Anexo de la Fase 1.

## 1. Alcance

Se evalúan cuatro estructuras, todas detrás de la misma interfaz primitiva `VertexHeap` (`int id`, `long key`).

| Código | Estructura | Rol | Representación (v1.2) |
| --- | --- | --- | --- |
| HH | Two-Parent Hollow Heap | Objeto de estudio | Nodos enlazados + `nodeOf[]` (id → nodo activo) |
| FH | Fibonacci Heap | Línea base teórica (misma cota amortizada) | Nodos enlazados + `nodeOf[]` |
| IBH | Indexed Binary Heap | Línea base práctica fuerte (decreaseKey real, tamaño ≤ \|V\|) | Solo arreglos: `long[] keys`, `int[] heap`, `int[] pos` |
| PQL | `java.util.PriorityQueue` con lazy insertion | Línea base "lo que escribiría un desarrollador Java" | `PriorityQueue<Entry(long key, int id)>` + `long[] dist` + `byte[] state` |

Plataforma: Eclipse Temurin 21.0.x. Colector principal G1; ParallelGC como control.

## 2. Preguntas de investigación

- **RQ1 (micro-operaciones).** ¿Cuál es el costo de `insert`, `decreaseKey` y `deleteMin` en cada estructura, tanto aislado como amortizado en secuencias mixtas?
- **RQ2 (Dijkstra).** ¿Qué determina qué estructura gana en Dijkstra: la densidad del grafo o el ratio `decreaseKey/deleteMin`? ¿Existe un punto de quiebre y dónde está?
- **RQ3 (memoria y GC).** ¿Qué huella por elemento, tasa de asignación y comportamiento de pausas de G1 produce cada estructura, y cómo influyen los nodos hollow?
- **RQ4 (punteros vs. arreglos).** ¿Cuánto del resultado se explica por localidad de caché y representación de punteros (CompressedOops activado/desactivado, fallos de L1/LLC)?

## 3. Definiciones y variables

- **Ratio de actualización** r = N\_dk / N\_dm, donde N\_dk es el número de `decreaseKey` y N\_dm el de `deleteMin` en una ejecución. Se mide con los contadores de la instrumentación (D5), no se asume.
- **Elemento:** un id entero en \[0, capacity). En Dijkstra, el id es el número de vértice. Cada id se inserta como máximo una vez por instancia de heap.
- **Ítems activos** ≤ |V|. **Nodos totales** (HH) = activos + hollow aún no liberados. HH **no** mantiene un máximo de |V| nodos: solo de ítems activos.
- **Tamaño máximo de cola en PQL** = número de relajaciones exitosas + 1 (con lazy insertion). Se mide con `physicalSize()`; no se asume que sea |E|.
- **Densidad** d = |E|/|V|. Se reporta siempre junto al par (|V|, |E|). La familia densa se define por la grilla de la sección 6, no por una fórmula en |V|.

## 4. Criterio de decisión (común a todas las hipótesis)

Para cada configuración c y métrica m se calcula la razón de medianas ρ\_c = m̃\_A / m̃\_B, con intervalo de confianza bootstrap al 95% (remuestreo jerárquico por fork e iteración, siguiendo Kalibera & Jones 2013).

- **Diferencia relevante:** ρ\_c ≥ 1,10 o ρ\_c ≤ 0,90, y el IC excluye 1,0.
- **Equivalencia:** el IC completo queda dentro de \[0,90; 1,10\].
- Una hipótesis "A supera a B en la región R" se **sostiene** si hay diferencia relevante a favor de A en al menos el 75% de las configuraciones de R y en ninguna hay diferencia relevante en contra. Si no se cumple, se declara **refutada** o **no concluyente** según el caso.
- Se reporta además el tamaño del efecto (δ de Cliff) y se aplica la corrección de Holm si se hacen pruebas formales entre pares.
- Los umbrales (10%, 75%) solo pueden ajustarse durante el piloto (F4), antes de tomar datos definitivos, y el cambio queda registrado en la sección 13.

## 5. Hipótesis

**H1 (RQ1). Latencia de `decreaseKey` y costo amortizado, HH vs. FH.**

- **H1a:** la dispersión de la latencia de `decreaseKey` aislado (razón p99/mediana) de HH es menor que la de FH, porque HH hace trabajo acotado en el peor caso y FH puede encadenar cortes.
- **H1b:** en secuencias mixtas (k `decreaseKey` seguidos de un `deleteMin`, con k ∈ {1, 4, 16, 64}), el costo amortizado por operación de HH no es peor que el de FH en más de un 10%. Se mide de forma amortizada porque `decreaseKey` de HH asigna un nodo nuevo y traslada trabajo a `deleteMin`, mientras que FH no asigna en `decreaseKey`.
- Se reporta también la mediana de la latencia aislada, sin hipótesis direccional.

**H2 (RQ2). Qué gana en Dijkstra.**

- **H2a:** en grafos aleatorios dispersos, medios y densos, r es pequeño e IBH o PQL es la estructura más rápida. HH y FH no superan a IBH de forma relevante.
- **H2b:** el tamaño máximo de cola de PQL en grafos aleatorios es mucho menor que |E| (se espera del orden de |V| más un factor logarítmico).
- **H2c:** existe un valor crítico de r por encima del cual HH supera a IBH, y se observa en las familias adversariales. El punto de quiebre se reporta como r\* con su intervalo, no como un valor de densidad.
- Si H2a se cumple y H2c no, se reporta como resultado negativo.

**H3 (RQ3). Memoria y GC.** Las cifras absolutas son resultados, no hipótesis.

- **H3a (reformulada en v1.2):** los bytes retenidos por ítem activo de HH (JOL, con CompressedOops) dependen de N\_dk. Sin `decreaseKey`, HH es comparable o menor que FH, porque su nodo tiene menos campos de referencia. Con `decreaseKey`, HH supera a FH en cuanto los nodos hollow se acumulan entre `deleteMin`. Se verifica con `ClassLayout` y `GraphLayout`, barriendo k en las secuencias mixtas.
- **H3b:** la tasa de asignación (B/op, vía `-prof gc`) de HH crece con N\_dk, mientras que la de FH no depende de N\_dk en `decreaseKey`.
- **H3c:** en cargas con r alto, los nodos hollow sobreviven hasta el siguiente `deleteMin`, lo que aumenta los bytes promovidos a old gen y las pausas p99 de G1 de HH frente a FH. Se contrasta con ParallelGC para separar el costo del colector del costo del algoritmo. El mecanismo propuesto es el de live set y promoción.

**H4 (RQ4). Punteros vs. arreglos.**

- **H4a:** por operación, IBH muestra menos fallos de LLC que HH y FH cuando el heap excede la caché L2 (verificable con `perfnorm`).
- **H4b:** desactivar CompressedOops degrada más a HH y FH que a IBH (interacción estructura × CompressedOops). En v1.2, IBH no tiene ninguna referencia por elemento, así que el contraste es directo.

## 6. Diseño experimental

**Factores**

| Factor | Niveles |
| --- | --- |
| Estructura | HH, FH, IBH, PQL |
| Tamaño n (micro) | 10³, 10⁴, 10⁵, 10⁶ |
| Secuencia (micro) | solo insert; insert + deleteMin; k decreaseKey + 1 deleteMin (k ∈ {1, 4, 16, 64}) |
| Familia de grafo | G1 dispersa, G2 media, G3 densa, G4 adversarial, G5 real |
| GC | G1 (principal), Parallel (control) |
| CompressedOops | on, off |

**Grilla de grafos** (se fija en F3; aquí, el diseño)

- **G1 dispersa:** |E| = 2|V|, con |V| ∈ {10⁴, 10⁵, 10⁶}.
- **G2 media:** |E| = |V| log₂|V|, mismos |V| (10⁶ solo si cabe en memoria).
- **G3 densa:** |E| = 0,1|V|², con |V| ∈ {2·10³, 5·10³, 10⁴, 2·10⁴}.
- **G4 adversarial:** familias que fuerzan un r alto, basadas en Cherkassky, Goldberg & Radzik (1996). Las definiciones exactas se verifican en la fuente durante F3.
- **G5 real:** redes viales del 9th DIMACS Challenge y grafos de SNAP, para validez externa.
- Grafos en formato CSR, pesos enteros no negativos, semilla fija y hash del dataset registrado. Distancias en `long`.

**Métricas:** tiempo por operación (ns/op), tiempo total de Dijkstra (ms), contadores de operaciones (N\_ins, N\_dk, N\_dm, tamaño máximo de cola), B/op y MB/s (`-prof gc`), pausas de G1 (`-Xlog:gc*`), bytes por nodo y huella retenida (JOL), fallos de L1/LLC y ciclos por operación (`perfnorm`).

**Entorno:** `-Xms = -Xmx`, `-XX:+AlwaysPreTouch`, JMH con ≥ 3 forks y warmup caracterizado en el piloto. Hardware, SO, versión exacta de Temurin, flags y semillas se documentan en `ENVIRONMENT.md`. Si el hardware no permite `perfnorm` (por ejemplo, sin Linux nativo), RQ4 se limita a la ablación de CompressedOops y se declara como limitación. Todo heap se crea con `capacity` = n (o |V|) antes de medir.

**Validez de correctitud:** differential testing contra un oráculo (secuencias aleatorias; se compara la secuencia de claves extraídas, no los ids), verificador de invariantes (`checkInvariants()`), cobertura y mutation testing, y verificación cruzada de las distancias de Dijkstra entre las cuatro estructuras. El número de secuencias y los puntajes se reportan; 10⁵ secuencias es un mínimo, no un resultado.

## 7. Outline del paper

1. **Introduction:** motivación, el problema de `decreaseKey` en Java (lazy insertion vs. decreaseKey real), RQ1–RQ4 y contribuciones (redactadas al final, según resultados).
2. **Background and Related Work:** HH, FH, IBH/PQL; Larkin et al. (2014) como metodología, no como evaluación de HH; justificación de Java 21 y G1 sujeta a la búsqueda bibliográfica de la sección 10.
3. **Experimental Setup:** entorno, API común `VertexHeap` y por qué es primitiva (sin boxing), familias de grafos, grilla y marco de medición (JMH).
4. **Correctness Validation:** differential testing, invariantes, cobertura y mutación.
5. **Statistical Methodology:** criterio de decisión de la sección 4, bootstrap y tamaño del efecto.
6. **Results and Discussion:** 6.1 micro-operaciones (RQ1), 6.2 Dijkstra y ratio r (RQ2), 6.3 memoria y GC (RQ3), 6.4 punteros vs. arreglos (RQ4).
7. **Threats to Validity:** interna (ruido, JIT, afinidad), externa (otras JVM, otros algoritmos, `long` vs. `int`, **uso con handles genéricos tipo librería no evaluado**), de constructo (representatividad de los grafos) y de implementación (calidad de las implementaciones propias).
8. **Conclusion and Future Work:** recomendaciones según r y entorno; como trabajo futuro, una versión concurrente, otros colectores (ZGC, Shenandoah) y la API genérica con handles.

**Abstract:** se redacta al final (F7) a partir de los resultados. No se fijan cifras ni conclusiones antes de medir.

## 8. Plan de fases y work packages

### Fase 0: Diseño del estudio y congelamiento metodológico (WP0)

- **Objetivo:** definir la matriz experimental, las métricas, los factores de control y las preguntas de investigación antes de escribir el código de benchmark.
- **Entregable:** este documento, con hipótesis congeladas tras cerrar los pendientes de la sección 10.

### Fase 1: Arquitectura e implementación base (WP1)

- **Objetivo:** cuatro estructuras comparables de forma justa en Java 21 Temurin.
- **Acciones:**
    - Interfaz primitiva `VertexHeap` (`int id`, `long key`), sin genéricos ni handles. **Hecho.**
    - Conversión de `HollowHeap` y `FibonacciHeap` a `VertexHeap`, con `nodeOf[]` y centinela `EXTRACTED`. **Hecho; en auditoría.**
    - `IndexedBinaryHeap` en arreglos puros y `JavaPriorityQueueAdapter` con lazy insertion. **Hecho; en auditoría.**
    - Política de asignación idéntica: buffers de `deleteMin` reutilizados en HH y FH; capacidad fija en HH, FH e IBH; `PriorityQueue` sin ajustes. **Hecho.**
    - Smoke test de la conversión (no sustituye a la Fase 2). **Hecho.**
    - Instrumentación por flag (D5). **Pendiente.**
    - `pom.xml` con jqwik, JOL y JMH (este último en módulo o perfil aparte). **Pendiente.**
    - Dijkstra sobre `VertexHeap` y un lector CSR mínimo, necesarios para la verificación cruzada de la Fase 2. **Pendiente.**

### Fase 2: Verificación rigurosa de correctitud (WP2)

- **Objetivo:** demostrar que los heaps cumplen sus invariantes y no tienen errores de punteros.
- **Acciones:**
    - **Unit testing (JUnit 5):** casos borde (heap vacío, claves duplicadas, `decreaseKey` en la raíz y en nodos internos, extracciones masivas, contrato de `VertexHeap`).
    - **Differential y property-based testing (jqwik):** las 4 estructuras contra un oráculo sobre al menos 10⁵ secuencias aleatorias. Se compara la secuencia de claves extraídas.
    - **Verificador de invariantes (`checkInvariants()`):** rangos, punteros `ep`, enlaces `child`/`next`, marcas de FH, propiedad de heap y coherencia de `nodeOf[]`/`pos[]`, tras cada operación en modo test.
    - **Cobertura y mutación** (JaCoCo, PIT).
    - **Verificación cruzada de Dijkstra:** distancias idénticas entre las cuatro estructuras.

### Fase 3: Generación de cargas y grafos (WP3)

- **Objetivo:** datasets para micro-operaciones y para Dijkstra.
- **Acciones:** grafos sintéticos G1–G3 en CSR con semilla fija; familias adversariales G4 que maximicen `decreaseKey`; importación de G5 (DIMACS, SNAP); hash de cada dataset.

### Fase 4: Calibración del harness y piloto (WP4)

- **Objetivo:** que las mediciones midan la estructura y no el entorno.
- **Acciones:** JMH con 5 iteraciones de warmup, 10 de medición y 3 forks; caracterizar el calentamiento de C2; flags base `-Xms4g -Xmx4g -XX:+UseG1GC -XX:+UseCompressedOops`; ajustar umbrales de la sección 4 si hace falta (con registro).

### Fase 5: Campaña experimental definitiva (WP5)

- ns/op aislado con JMH para `insert`, `decreaseKey` y `deleteMin`.
- Tiempo total de Dijkstra por familia de grafo.
- Huella con JOL (`ClassLayout`, `GraphLayout`).
- Tasa de asignación y pausas de GC (`-prof gc`, `-Xlog:gc*`).
- Ablación con `-XX:-UseCompressedOops`; `perfnorm` si el hardware lo permite.

### Fase 6: Análisis estadístico (WP6)

- Pipeline en Python (`pandas`, `scipy`, `seaborn`).
- Medianas e IC bootstrap al 95%, δ de Cliff, Holm.
- Tests no paramétricos (Mann-Whitney U / Kruskal-Wallis).

### Fase 7: Manuscrito y paquete de reproducibilidad (WP7)

- Paper en LaTeX (IEEE/ACM) y contenedor con un único script que reproduce las figuras.

## 9. Estado de avance y reparto de tareas

La Fase 1 está casi cerrada: la conversión a `VertexHeap` compila y pasa el smoke test, y falta la auditoría, la instrumentación, el build completo y Dijkstra. Asignen un responsable por fila; una tarea puede empezar cuando sus dependencias estén en "Hecho".

| Id | Fase | Tarea | Entregable | Depende de | Responsable | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| T0.1 | F0 | Búsqueda del claim de novedad (Scholar, "cited by" de Hansen et al.; ALENEX, SEA, JEA) | Nota con los trabajos encontrados y la contribución ajustada | — |  | Pendiente |
| T0.2 | F0 | Verificar referencias (autores, año, DOI) | `.bib` verificado | — |  | Pendiente |
| T0.3 | F0 | Confirmar hardware (Linux nativo, RAM, `perf`) | `ENVIRONMENT.md` | — |  | Pendiente |
| T1.1 | F1 | Auditar HH línea por línea (`git diff`: solo deben cambiar tipos y nombres) | Visto bueno o lista de hallazgos | — |  | En curso |
| T1.2 | F1 | Auditar FH | Visto bueno o lista de hallazgos | — |  | En curso |
| T1.3 | F1 | Auditar IBH y PQL | Visto bueno o lista de hallazgos | — |  | En curso |
| T1.4 | F1 | Instrumentación por flag (D5): N\_ins, N\_dk, N\_dm, hollow creados/liberados, tamaño máximo | Contadores en las 4 clases | T1.1–T1.3 |  | Pendiente |
| T1.5 | F1 | Completar `pom.xml`: jqwik, JOL, JMH (módulo o perfil aparte) | Build con un solo comando | — |  | Pendiente |
| T1.6 | F1 | Dijkstra sobre `VertexHeap` + lector CSR mínimo | Clase `Dijkstra` y grafo de prueba | T1.1–T1.3 |  | Pendiente |
| T1.7 | F1 | Registrar decisiones v1.2 en este documento | Secciones 12 y Anexo | — |  | Hecho |
| T2.1 | F2 | Unit tests: casos borde y contrato de `VertexHeap` | Suite JUnit 5 | T1.5 |  | Pendiente |
| T2.2 | F2 | `checkInvariants()` en HH (rangos, `ep`, `child`/`next`, `nodeOf`) | Método + test | T1.1 |  | Pendiente |
| T2.3 | F2 | `checkInvariants()` en FH e IBH | Métodos + tests | T1.2, T1.3 |  | Pendiente |
| T2.4 | F2 | Differential + property-based (jqwik), ≥ 10⁵ secuencias | Suite jqwik y conteo reportado | T1.5, T2.2, T2.3 |  | Pendiente |
| T2.5 | F2 | Cobertura y mutación (JaCoCo, PIT) | Puntajes reportados | T2.1, T2.4 |  | Pendiente |
| T2.6 | F2 | Verificación cruzada de distancias de Dijkstra | Test de distancias idénticas | T1.6 |  | Pendiente |

**Reparto sugerido para tres personas**, con poco solapamiento: una persona toma HH (T1.1, T2.2); otra, FH, IBH y PQL (T1.2, T1.3, T2.3); la tercera, infraestructura (T1.5, T1.6, T0.3). T0.1 y T0.2 no dependen de nada y puede tomarlas quien termine primero.

## 10. Tareas pendientes de F0 antes de congelar

1. **Búsqueda del claim de novedad (T0.1):** buscar evaluaciones empíricas de Hollow Heaps (Google Scholar, "cited by" sobre Hansen et al.; ALENEX, SEA, JEA). Si existen, ajustar la contribución a "Java 21, G1 y análisis de memoria". No afirmar la ausencia de trabajos sin esta búsqueda.
2. **Verificar las referencias (T0.2):** autores, año y DOI de todo lo citado.
3. **Confirmar el hardware disponible (T0.3):** ¿Linux nativo para `perfnorm`? ¿Cuánta RAM para los grafos grandes?

## 11. Riesgos y planes de contingencia

| Riesgo | Plan |
| --- | --- |
| Los resultados contradicen las hipótesis | Se reportan como resultados negativos con el mismo rigor |
| Sin acceso a `perf` | RQ4 se reduce a la ablación de CompressedOops |
| Grafos G3 o G5 no caben en memoria | Se reduce \|V\| y se documenta |
| Tiempo insuficiente | Se recorta FH antes que la calidad del benchmark; el núcleo mínimo es HH, IBH y PQL |
| La auditoría encuentra cambios de lógica en la conversión | Se revierte ese archivo a la versión anterior y se repite la conversión solo con cambios de tipos |
| JOL no confirma el layout estimado de H3a | Se reporta el layout medido; H3a se evalúa con los bytes reales, sin cambiar su formulación |

## 12. Registro de cambios antes del congelamiento

### v1.1 → v1.2 (2026-10-29, Fase 1)

| Sección | Cambio | Motivo |
| --- | --- | --- |
| Anexo (D2) | Se elimina la capa genérica `MinHeap<K,V>`/`HeapHandle`. Queda una sola API primitiva `VertexHeap` (`int id`, `long key`) para microbenchmarks y Dijkstra | Los genéricos obligan al boxing (un `Long` por operación), que contamina la latencia (H1a), la asignación (H3b) y la promoción a old gen (H3c). `long` evita el desbordamiento de distancias (D1) |
| Anexo (D3) | HH y FH: `nodeOf[]` preasignado (id → nodo activo) con centinela `EXTRACTED`. IBH: `long[] keys`, `int[] heap`, `int[] pos`, sin objetos por elemento. PQL: `long[] dist` y `byte[] state`. HH, FH e IBH tienen capacidad fija; PQL usa `PriorityQueue` con capacidad por defecto y sí redimensiona (D4) | API idéntica para las cuatro; IBH queda como arreglo puro para RQ4 |
| 5 (H3a) | Reformulada en función de N\_dk: sin `decreaseKey`, HH es comparable o menor que FH; con `decreaseKey`, HH lo supera cuando se acumulan nodos hollow | Al eliminarse `HollowItem`, la premisa "dos objetos por elemento" deja de valer. Layout estimado con CompressedOops: nodo HH \~40 B, nodo FH \~48 B; se confirma con JOL |
| Anexo (contrato) | Contrato común de `VertexHeap` (ver Anexo) | Evita que en PQL una entrada obsoleta reviva al reinsertar un id; Dijkstra no reinserta vértices |
| 6 (correctitud) | FH desempata por id de vértice (antes, por orden de inserción); HH e IBH no desempatan. El oráculo compara la secuencia de claves, no los ids | Con claves repetidas, estructuras correctas pueden extraer ids distintos |
| 7 (amenazas) | El uso con handles genéricos (tipo librería) no se evalúa; pasa a trabajo futuro | Queda fuera de RQ1–RQ4 |
| 8 (Fase 1) | Acciones actualizadas a `VertexHeap`; se añaden instrumentación, build y Dijkstra como pendientes | Refleja el avance real |
| 9 (nueva) | Tabla de estado y reparto de tareas | Dividir el trabajo en el equipo |

### v1.0 → v1.1

1. Corregida la afirmación "HH mantiene tamaño máximo |V|": solo los ítems activos.
2. H3: eliminadas las cifras absolutas (32 B, 40 B) y el mecanismo de "referencias circulares"; reformulada con live set y promoción.
3. H2: el factor explicativo pasa de densidad a ratio r; la baseline principal pasa a ser IBH; se mide el tamaño real de la cola de PQL.
4. H1: se añade el costo amortizado en secuencias mixtas porque `decreaseKey` de HH asigna.
5. Añadida RQ4/H4 (punteros vs. arreglos, CompressedOops, caché).
6. Añadida la familia de grafos reales y definida la grilla (|V|, |E|).
7. Añadidos el criterio de decisión estadístico, la metodología estadística y la validación de correctitud.
8. Corregida la descripción de JMH (controla warmup y artefactos, no "aísla C2").
9. Larkin et al. (2014) citado como metodología; claim de novedad sujeto a verificación.
10. El abstract ya no presupone resultados.

## 13. Registro de enmiendas (después del congelamiento)

Vacío: el diseño aún no está congelado. Los cambios previos al congelamiento van en la sección 12.

| Fecha | Sección | Cambio | Motivo |
| --- | --- | --- | --- |
|  |  |  |  |

## Anexo: Fase 1, arquitectura e implementación

### A.1 Decisiones de diseño (v1.2)

| Id | Decisión | Resolución | Estado |
| --- | --- | --- | --- |
| D1 | Tipo de clave | `long` en todas las estructuras; no se mantiene variante `int` | Cerrada |
| D2 | Modelo de API | Una sola capa primitiva `VertexHeap` (`int id`, `long key`), usada en microbenchmarks y Dijkstra. Sin genéricos ni handles | Cerrada |
| D3 | Ubicar un elemento por id | HH y FH: `nodeOf[]` preasignado con centinela `EXTRACTED`. IBH: `int[] pos`. PQL: `long[] dist` + `byte[] state`. Todo cuenta en la huella de memoria. `HollowItem` eliminado | Cerrada |
| D4 | Política de asignación | Buffers de `deleteMin` reutilizados como campos de instancia en HH (`L`, `A`) y FH (`rootList`, `slots`), limpiados tras cada uso. `PriorityQueue` sin ajustes | Cerrada |
| D5 | Instrumentación | Contadores (N\_ins, N\_dk, N\_dm, hollow creados/liberados, tamaño máximo) activables por flag `static final`, medidos en una ejecución de verificación aparte, no durante las mediciones de tiempo | Pendiente (T1.4) |

### A.2 Interfaz común

```java
public interface VertexHeap {
    void insert(int id, long key);
    void decreaseKey(int id, long newKey);
    int  deleteMin();        // id del mínimo, -1 si está vacío
    int  findMin();          // id del mínimo, -1 si está vacío
    long keyOf(int id);      // IllegalStateException si el id no está en el heap
    boolean contains(int id);
    boolean isEmpty();
    int  size();
}
```

Cada estructura se construye con `(int capacity)`; los ids válidos son \[0, capacity).

### A.3 Contrato (idéntico en las cuatro)

| Situación | Comportamiento |
| --- | --- |
| `insert` con id fuera de rango | `IndexOutOfBoundsException` |
| `insert` de un id ya presente o ya extraído | `IllegalStateException` (cada id se inserta una sola vez) |
| `decreaseKey` / `keyOf` de un id ausente | `IllegalStateException` |
| `decreaseKey` con `newKey >= clave actual` | No hace nada |
| `deleteMin` / `findMin` con heap vacío | Devuelve −1 |
| Claves iguales | FH desempata por id; HH e IBH no desempatan. Los tests comparan claves, no ids |
| `findMin` en PQL | Descarta entradas obsoletas de la cima (efecto lateral, resultado correcto) |

### A.4 Estado por id

| Estructura | Nunca insertado | En el heap | Extraído |
| --- | --- | --- | --- |
| HH | `nodeOf[id] == null` | nodo activo | `nodeOf[id] == EXTRACTED` |
| FH | `nodeOf[id] == null` | nodo | `nodeOf[id] == EXTRACTED` |
| IBH | `pos[id] == -2` | `pos[id] >= 0` | `pos[id] == -1` |
| PQL | `state[id] == 0` | `state[id] == 1` | `state[id] == 2` |

### A.5 Notas para Threats to Validity y para el harness

- PQL asigna un `Entry` por cada `insert` y `decreaseKey`, y HH un nodo por `decreaseKey`. Son asignaciones propias de cada estructura y forman parte de lo que mide H3b. Lo que se eliminó fue el boxing.
- Todo benchmark crea el heap con `capacity` = n antes de medir.
- Bugs corregidos durante la unificación: en FH, la comparación de claves `Integer` con `!=` (comparaba referencias) y la actualización de `min` en `decreaseKey` sin el criterio de desempate.
