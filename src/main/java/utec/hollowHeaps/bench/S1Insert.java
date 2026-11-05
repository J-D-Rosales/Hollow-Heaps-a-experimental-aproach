package utec.hollowHeaps.bench;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import utec.hollowHeaps.api.VertexHeap;

/**
 * S1: n inserts con claves aleatorias sobre un heap vacío (RQ1, H3a).
 *
 * <p>Cada id se inserta una sola vez (contrato de VertexHeap), así que el heap se reconstruye
 * en cada invocación; JMH no mide el setup. El resultado es tiempo por invocación (n inserts):
 * el pipeline divide entre n para obtener ns/op.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 3, jvmArgsAppend = {"-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch", "-XX:+UseG1GC"})
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class S1Insert {

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
    public void freshHeap() {
        heap = Heaps.create(impl, n);
    }

    @Benchmark
    public void insertAll(Blackhole bh) {
        VertexHeap h = heap;
        long[] k = keys;
        for (int i = 0; i < k.length; i++) {
            h.insert(i, k[i]);
        }
        bh.consume(h);
    }
}
