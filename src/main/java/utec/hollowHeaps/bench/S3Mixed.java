package utec.hollowHeaps.bench;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * S3: rondas de "k decreaseKey + 1 deleteMin + 1 insert" con el heap en tamaño n
 * (H2a, H2b, H3, H4). La carga está en {@link S3Workload}.
 *
 * <p>Cada id se inserta una sola vez, así que el heap se reconstruye en cada invocación
 * (Level.Invocation, no medido). Cada invocación ejecuta S3Workload.ROUNDS rondas (≫ 1 ms).
 *
 * <p>Resultado: tiempo por invocación. En el análisis:
 * ns/op = score / (ROUNDS · (k + 2)), ns/ronda = score / ROUNDS.
 *
 * <p>Ojo con -prof gc: incluye lo que asigna el setup (el heap entero en cada invocación).
 * El B/op de la parte medida sola lo da {@link AllocProbe}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 3, jvmArgsAppend = {"-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch", "-XX:+UseG1GC"})
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class S3Mixed {

    @Param({"HH", "FH", "IBH", "PQL"})
    public String impl;

    @Param({"10000", "1000000"})
    public int n;

    @Param({"0", "1", "4", "16", "64"})
    public int k;

    private S3Workload w;

    @Setup(Level.Trial)
    public void generate() {
        w = new S3Workload(impl, n, k);
    }

    @Setup(Level.Invocation)
    public void freshHeap() {
        w.reset();
    }

    @Benchmark
    public void mixed(Blackhole bh) {
        bh.consume(w.run());
    }

    @TearDown(Level.Invocation)
    public void check() {
        w.check();
    }
}
