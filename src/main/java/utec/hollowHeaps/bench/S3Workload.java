package utec.hollowHeaps.bench;

import java.util.SplittableRandom;

import utec.hollowHeaps.api.VertexHeap;

/**
 * Carga del escenario S3, compartida por {@link S3Mixed} (tiempo), {@link JolFootprint}
 * (bytes por elemento) y {@link AllocProbe} (B/op sin el setup), para que los tres midan
 * exactamente la misma secuencia de operaciones.
 *
 * <p>Heap con n elementos; ROUNDS rondas de "k decreaseKey sobre ids vivos al azar +
 * 1 deleteMin + 1 insert de un id nuevo". El tamaño se mantiene en n.
 *
 * <p>Claves (sección 3 del diseño, como en Dijkstra):
 * <ul>
 *   <li>decreaseKey: clave actual − U[1, DEC_DELTA], sin bajar de la última clave extraída;</li>
 *   <li>insert: última clave extraída + U[0, INS_DELTA).</li>
 * </ul>
 *
 * <p>Toda la aleatoriedad se pregenera en el constructor (semilla fija), así que el RNG no
 * entra en la medición y todas las invocaciones repiten la misma secuencia. Las claves
 * viven en un arreglo del harness ({@code key[]}) para no medir {@code keyOf}.
 */
public final class S3Workload {

    /** Rondas por invocación. Ajustar en el piloto (F4) si la campaña no cabe en tiempo. */
    public static final int ROUNDS = 100_000;
    /** Claves iniciales: U[0, INIT_RANGE). */
    public static final long INIT_RANGE = 1L << 30;
    /** decreaseKey resta U[1, DEC_DELTA]. */
    public static final int DEC_DELTA = 1 << 10;
    /** insert usa lastKey + U[0, INS_DELTA). */
    public static final int INS_DELTA = 1 << 30;
    static final long SEED = 42L;

    public final String impl;
    public final int n, k, rounds;

    // aleatoriedad pregenerada
    private final long[] initKeys;
    private final int[] pickPos;   // posición en live[] del id a decrementar, rounds·k
    private final int[] decDelta;  // rounds·k
    private final int[] insDelta;  // rounds
    private final int prerollDelta;

    // estado
    private VertexHeap heap;
    private long[] key;            // clave actual por id
    private int[] live;            // ids vivos, live[0..n)
    private int[] where;           // where[id] = posición en live[]
    private int nextId;
    private long lastKey;

    public S3Workload(String impl, int n, int k) {
        this(impl, n, k, ROUNDS);
    }

    public S3Workload(String impl, int n, int k, int rounds) {
        this.impl = impl;
        this.n = n;
        this.k = k;
        this.rounds = rounds;
        SplittableRandom rnd = new SplittableRandom(SEED);
        initKeys = new long[n];
        for (int i = 0; i < n; i++) initKeys[i] = rnd.nextLong(INIT_RANGE);
        int total = rounds * k;
        pickPos = new int[total];
        decDelta = new int[total];
        for (int i = 0; i < total; i++) {
            pickPos[i] = rnd.nextInt(n);
            decDelta[i] = 1 + rnd.nextInt(DEC_DELTA);
        }
        insDelta = new int[rounds];
        for (int i = 0; i < rounds; i++) insDelta[i] = rnd.nextInt(INS_DELTA);
        prerollDelta = rnd.nextInt(INS_DELTA);
    }

    /**
     * Setup (no medido): heap nuevo con n elementos y una ronda de precalentamiento
     * (deleteMin + insert) para que FH consolide su lista de n raíces fuera de la medición.
     */
    public void reset() {
        int cap = n + rounds + 1;
        heap = Heaps.create(impl, cap);
        key = new long[cap];
        where = new int[cap];
        live = new int[n];
        for (int i = 0; i < n; i++) {
            key[i] = initKeys[i];
            heap.insert(i, key[i]);
            live[i] = i;
            where[i] = i;
        }
        nextId = n;
        lastKey = 0;
        replaceMin(prerollDelta);
    }

    /** La parte medida: todas las rondas. Devuelve la suma de ids extraídos (para Blackhole). */
    public long run() {
        final VertexHeap h = heap;
        final long[] key = this.key;
        final int[] live = this.live;
        final int[] pickPos = this.pickPos, decDelta = this.decDelta, insDelta = this.insDelta;
        final int k = this.k, rounds = this.rounds;
        long sum = 0;
        int p = 0;
        for (int r = 0; r < rounds; r++) {
            for (int j = 0; j < k; j++, p++) {
                int id = live[pickPos[p]];
                long nk = Math.max(lastKey, key[id] - decDelta[p]);
                key[id] = nk;
                h.decreaseKey(id, nk);     // no-op si nk == clave actual (contrato)
            }
            sum += replaceMin(insDelta[r]);
        }
        return sum;
    }

    /** deleteMin + insert de un id nuevo en el hueco de live[] que deja el extraído. */
    private int replaceMin(int delta) {
        int m = heap.deleteMin();
        lastKey = key[m];
        int id = nextId++;
        int slot = where[m];
        live[slot] = id;
        where[id] = slot;
        key[id] = lastKey + delta;
        heap.insert(id, key[id]);
        return m;
    }

    public VertexHeap heap() {
        return heap;
    }

    /** Comprobación fuera de la medición: el tamaño debe seguir siendo n. */
    public void check() {
        if (heap.size() != n) {
            throw new IllegalStateException(impl + ": size " + heap.size() + " != n " + n);
        }
    }

    /** Operaciones por invocación de run(): k decreaseKey + deleteMin + insert por ronda. */
    public long opsPerRun() {
        return (long) rounds * (k + 2);
    }
}
