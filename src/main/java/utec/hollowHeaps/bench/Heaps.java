package utec.hollowHeaps.bench;

import utec.hollowHeaps.adapter.JavaPriorityQueueAdapter;
import utec.hollowHeaps.api.VertexHeap;
import utec.hollowHeaps.binary.IndexedBinaryHeap;
import utec.hollowHeaps.fibonacci.FibonacciHeap;
import utec.hollowHeaps.hollow.HollowHeap;

/** Fábrica común de los benchmarks: crea la estructura indicada por su código del documento de diseño. */
public final class Heaps {

    private Heaps() {}

    public static VertexHeap create(String impl, int capacity) {
        return switch (impl) {
            case "HH"  -> new HollowHeap(capacity);
            case "FH"  -> new FibonacciHeap(capacity);
            case "IBH" -> new IndexedBinaryHeap(capacity);
            case "PQL" -> new JavaPriorityQueueAdapter(capacity);
            default -> throw new IllegalArgumentException("Estructura desconocida: " + impl);
        };
    }

    /** Claves aleatorias reproducibles en [0, 2^30). */
    public static long[] randomKeys(int n, long seed) {
        java.util.SplittableRandom rnd = new java.util.SplittableRandom(seed);
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) keys[i] = rnd.nextLong(1L << 30);
        return keys;
    }
}
