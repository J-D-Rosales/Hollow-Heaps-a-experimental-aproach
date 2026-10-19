# Plan de Trabajo para Equipo (Contenido para el `README.md`)

Este desglose está estructurado en **Paquetes de Trabajo (Work Packages - WP)** para repartir tareas entre los integrantes del equipo.

---

## Work Package 1: Arquitectura y Estructuras de Datos (`com.benchmark.heap`)

**Objetivo:** Tener las 4 estructuras operativas, probadas con tests unitarios y acopladas a la interfaz común.

* [ ] **1.1 Interfaz Unificada y Handle:**
* Crear `MinHeap<K, V>` y `HeapHandle<K, V>`.
* Asegurar que el handle permita `decreaseKey` en $O(1)$ sin exponer nodos internos.


* [ ] **1.2 Implementación `HollowHeap`:**
* Finalizar `insert`, `deleteMin`, `decreaseKey` (soporte dos padres / DAG) según Hansen et al. (2017).


* [ ] **1.3 Implementación `FibonacciHeap`:**
* Implementar o adaptar Fibonacci Heap (punteros dobles circulares, `cascadingCut`, consolidación por grado).


* [ ] **1.4 Adaptadores `IndexedBinaryHeap` y `JavaPriorityQueue`:**
* Implementar `IndexedBinaryHeap` ($O(\log n)$ con tabla hash o arreglo auxiliar para ubicar nodos).
* Crear el adaptador para `java.util.PriorityQueue` usando la técnica de *Lazy Insertion* (duplicados permitidos).


* [ ] **1.5 Suite de Pruebas Unitarias (JUnit 5):**
* Verificar correctness en las 4 estructuras: inserción masiva, extracciones consecutivas de mínimos y reducciones de clave complejas.



---

## Work Package 2: Motor de Grafos y Dijkstra (`com.benchmark.graph`)

**Objetivo:** Construir el macro-benchmark real sobre el algoritmo de Dijkstra.

* [ ] **2.1 Generador de Grafos Sintéticos (`GraphGenerator`):**
* Implementar generador de grafos dirigidos con pesos no negativos ($w \ge 0$).
* Permitir configurar el número de vértices $\vert{}V\vert{}$ y densidad de aristas $\vert{}E\vert{}$:
* **Disperso:** $\vert{}E\vert{} = 2\vert{}V\vert{}$ (Mapas viales).
* **Medio:** $\vert{}E\vert{} = \vert{}V\vert{} \log_2 \vert{}V\vert{}$.
* **Denso:** $\vert{}E\vert{} = 0.1 \times \vert{}V\vert{}^2$.


* Usar una **semilla fija (`Seed`)** en el generador aleatorio para asegurar reproducibilidad exacta.


* [ ] **2.2 Algoritmo de Dijkstra Genérico (`DijkstraEngine`):**
* Implementar Dijkstra de modo que reciba `MinHeap<Integer, Integer>` como parámetro.
* Medir únicamente el tiempo de ejecución del cálculo de distancias (excluyendo la construcción inicial del grafo).



---

## Work Package 3: Benchmarking, Perfilamiento y Métricas JVM (`com.benchmark.bench`)

**Objetivo:** Recolectar datos numéricos libres de sesgo utilizando herramientas oficiales de la JVM.

* [ ] **3.1 Suite de Micro-benchmarks con JMH:**
* Configurar JMH para medir operaciones aisladas en nanosegundos/op:
* Benchmark 1: `insert` masivo ($N = 10^3$ a $10^6$).
* Benchmark 2: `decreaseKey` intensivo ($10\%$ de extracciones, $90\%$ de reducciones de clave).
* Benchmark 3: `deleteMin` hasta vaciar el heap.




* [ ] **3.2 Análisis de Memoria Nodal con JOL:**
* Ejecutar `ClassLayout.parseClass()` sobre `HollowNode`, `HollowItem`, `FibonacciNode` e `IndexedNode`.
* Documentar bytes consumidos en memoria por nodo (incluyendo *Headers*, *Padding* y *CompressedOops*).


* [ ] **3.3 Monitoreo del Garbage Collector (GC) y Memoria Heap:**
* Configurar argumentos JVM: `-Xms4g -Xmx4g -XX:+UseG1GC -Xlog:gc*,gc+phases=debug:file=gc.log`.
* Medir tasa de asignación de memoria (MB/s) y pausas del GC con **async-profiler** o métricas de JMH (`-prof gc`).



---

## Work Package 4: Procesamiento de Datos y Gráficos (`scripts/`)

**Objetivo:** Automatizar la generación de tablas y gráficos con calidad de publicación.

* [ ] **4.1 Exportación de Datos:**
* Configurar JMH y las pruebas de Dijkstra para guardar resultados en archivos `.csv` en la carpeta `results/`.


* [ ] **4.2 Scripts de Visualización (Python - Matplotlib / Seaborn):**
* Crear script `plot_microbenchmarks.py`: Tiempo por operación vs. $N$.
* Crear script `plot_dijkstra.py`: Tiempo de Dijkstra vs. Densidad del Grafo ($\vert{}E\vert{}/\vert{}V\vert{}$).
* Crear script `plot_memory.py`: Huella de memoria y tasa de asignación de GC.



---

## Work Package 5: Redacción del Artículo Académico (`paper/`)

**Objetivo:** Redactar el manuscrito en formato IEEE / ACM (LaTeX).

---

# Estructura del Paper y Puntos de Concentración

Para que el manuscrito sea riguroso, el equipo debe enfocar la redacción en las siguientes 7 secciones principales:

```text
paper/
├── main.tex               <-- Documento principal en LaTeX
├── references.bib         <-- Bibliografía (Tarjan 2017, Larkin 2014, etc.)
└── figures/               <-- Gráficos generados por el WP 4

```

### Secciones Clave del Documento

| Sección | Enfoque de Contenido y Tareas del Equipo |
| --- | --- |
| **Title & Abstract** | Enunciado claro del problema (memoria contigua vs. $O(1)$ amortizado), metodología usada (Java 21, JMH, Dijkstra) y **resultado principal cuantificado** (ej. *"Hollow Heap superó a Fibonacci Heap en un 42% en tiempo de ejecución..."*). |
| **1. Introduction** | Motivación histórica + Presentar las **3 Preguntas de Investigación (RQ1, RQ2, RQ3)** definidas en la metodología. |
| **2. Theoretical Background** | Matriz comparativa de complejidades teóricas y descripción estructural de cada Heap (DAGs perezosos vs. listas dobles circulares vs. arreglos). |
| **3. Memory & JVM Analysis** | Presentación de los resultados de **JOL (Java Object Layout)**. Demostrar cuántos bytes ocupa cada nodo en la memoria Heap de Java 21 y calcular la presión teórica sobre la caché L1/L2. |
| **4. Experimental Setup** | Especificación exacta del hardware (CPU, cachés, RAM), versión de la JVM (`Eclipse Temurin 21.0.x`), flags del compilador, configuración de JMH y generador de grafos. **Sección crítica para la reproducibilidad.** |
| **5. Results & Discussion** | **El núcleo del paper.** Presentación de los gráficos de JMH y Dijkstra. Discusión enfocada en explicar *por qué* ocurre cada resultado (relacionando el rendimiento con los *cache misses*, el *overhead* de punteros y las pausas del GC). |
| **6. Threats to Validity** | Factores que podrían alterar los datos: optimizaciones agresivas del JIT C2 (*Escape Analysis*, *Inlining*), variaciones térmicas de la CPU y pausas inevitables del sistema operativo. |
| **7. Conclusion** | Veredicto final respaldado por los datos experimentales y guía de recomendaciones técnicas sobre cuándo usar cada estructura en Java. |

---

### Siguiente paso para el equipo

1. Copiar este plan directamente al `README.md` del repositorio del proyecto.
2. Asignar las tareas de los **Work Packages 1 y 2** entre los integrantes para tener el código compilable antes de comenzar la fase de medición con JMH.