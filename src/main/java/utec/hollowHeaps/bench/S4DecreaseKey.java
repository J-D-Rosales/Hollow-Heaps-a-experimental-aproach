package utec.hollowHeaps.bench;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import utec.hollowHeaps.api.VertexHeap;

/**
 * S4: decreaseKey aislado sobre ids vivos no raíz, en modo SampleTime (H1: p99/mediana).
 *
 * <p>Para que ningún decreaseKey caiga en la raíz sin consultar findMin en la medición, se
 * inserta un id centinela con clave 0 que es siempre el mínimo; las demás claves empiezan
 * en BASE + U[0, 2^30) y nunca bajan de 1.
 *
 * <p>Sin deleteMin, HH acumula un nodo hollow por decreaseKey y PQL una entrada por
 * decreaseKey, y los árboles de FH se van aplanando por los cortes. Por eso el heap se
 * reconstruye en cada iteración (Level.Iteration) y las iteraciones son cortas (1 s).
 * Tras construirlo se hace un deleteMin + insert (fuera de la medición) para que FH y HH
 * tengan árboles, no solo una lista de raíces.
 *
 * <p>El score de SampleTime ya es ns por decreaseKey; JMH reporta p0.50, p0.99, etc.
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 3, jvmArgsAppend = {"-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch", "-XX:+UseG1GC"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class S4DecreaseKey {

    static final long BASE = 1L << 40;
    static final int DEC_DELTA = 1 << 10;
    /** Tamaño del flujo pregenerado de (id, delta); se recorre de forma cíclica. */
    static final int STREAM = 1 << 20;

    @Param({"HH", "FH", "IBH", "PQL"})
    public String impl;

    @Param({"10000", "1000000"})
    public int n;

    private int[] targets;    // ids a decrementar (nunca el centinela)
    private int[] deltas;
    private long[] initKeys;

    private VertexHeap heap;
    private long[] key;
    private int cursor;

    @Setup(Level.Trial)
    public void generate() {
        SplittableRandom rnd = new SplittableRandom(42L);
        initKeys = new long[n];
        for (int i = 0; i < n; i++) initKeys[i] = BASE + rnd.nextLong(1L << 30);
        targets = new int[STREAM];
        deltas = new int[STREAM];
        for (int i = 0; i < STREAM; i++) {
            targets[i] = rnd.nextInt(n);          // ids 0..n-1; el centinela es n
            deltas[i] = 1 + rnd.nextInt(DEC_DELTA);
        }
    }

    @Setup(Level.Iteration)
    public void freshHeap() {
        int sentinel = n, extra = n + 1, cap = n + 2;
        heap = Heaps.create(impl, cap);
        key = new long[cap];
        for (int i = 0; i < n; i++) {
            key[i] = initKeys[i];
            heap.insert(i, key[i]);
        }
        // Consolidación: insertar un mínimo temporal y extraerlo arma los árboles de FH/HH.
        key[extra] = 0;
        heap.insert(extra, 0);
        heap.deleteMin();
        // Centinela: raíz permanente.
        key[sentinel] = 0;
        heap.insert(sentinel, 0);
        cursor = 0;
    }

    @Benchmark
    public void decreaseKey() {
        int c = cursor;
        int id = targets[c];
        long nk = Math.max(1L, key[id] - deltas[c]);
        key[id] = nk;
        heap.decreaseKey(id, nk);
        cursor = (c + 1) & (STREAM - 1);
    }

    /** Fuera de la medición: el centinela debe seguir siendo el mínimo. */
    @TearDown(Level.Iteration)
    public void check() {
        if (heap.findMin() != n || heap.size() != n + 1) {
            throw new IllegalStateException(impl + ": centinela perdido o tamaño incorrecto");
        }
    }
}
