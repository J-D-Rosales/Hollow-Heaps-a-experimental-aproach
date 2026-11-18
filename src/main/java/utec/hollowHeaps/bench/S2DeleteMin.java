package utec.hollowHeaps.bench;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import utec.hollowHeaps.api.VertexHeap;

/**
 * S2: heap con n elementos (insertados en el setup, no medidos) que se vacía con deleteMin
 * (RQ1, H4a).
 *
 * <p>El resultado es tiempo por invocación (n deleteMin): el pipeline divide entre n.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 3, jvmArgsAppend = {"-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch", "-XX:+UseG1GC"})
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class S2DeleteMin {

    @Param({"HH", "FH", "IBH", "PQL"})
    public String impl;

    @Param({"10000", "1000000"})
    public int n;

    private long[] keys;
    private VertexHeap heap;

    @Setup(Level.Trial)
    public void generateKeys() {
        keys = Heaps.randomKeys(n, 42L);
    }

    @Setup(Level.Invocation)
    public void filledHeap() {
        heap = Heaps.create(impl, n);
        for (int i = 0; i < n; i++) {
            heap.insert(i, keys[i]);
        }
    }

    @Benchmark
    public void drain(Blackhole bh) {
        VertexHeap h = heap;
        int id;
        while ((id = h.deleteMin()) != -1) {
            bh.consume(id);
        }
    }

    /** Fuera de la medición: comprueba que el heap quedó vacío. */
    @TearDown(Level.Invocation)
    public void check() {
        if (!heap.isEmpty()) {
            throw new IllegalStateException(impl + ": el heap no quedó vacío tras drain()");
        }
    }
}
