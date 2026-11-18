package utec.hollowHeaps.bench;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.sun.management.ThreadMXBean;

/**
 * H3b: bytes asignados por operación en S3, contando SOLO la parte medida (las rondas),
 * no el setup. Complementa a -prof gc, que en S3 también cuenta el heap que se reconstruye
 * en cada invocación.
 *
 * <p>Usa getThreadAllocatedBytes del hilo actual alrededor de {@link S3Workload#run()}.
 * Repite REPS veces por configuración, descarta las primeras WARMUP (JIT) y reporta la
 * mediana, el mínimo y el máximo.
 *
 * <pre>java -Xms4g -Xmx4g -XX:+UseG1GC -cp target/benchmarks.jar utec.hollowHeaps.bench.AllocProbe results/alloc.csv</pre>
 */
public final class AllocProbe {

    static final int WARMUP = 5, REPS = 15;

    public static void main(String[] args) throws IOException {
        Path csv = Path.of(args.length > 0 ? args[0] : "results/alloc.csv");
        if (csv.getParent() != null) Files.createDirectories(csv.getParent());
        ThreadMXBean mx = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        mx.setThreadAllocatedMemoryEnabled(true);
        long tid = Thread.currentThread().getId();
        long blackhole = 0;

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(csv))) {
            out.println("impl,n,k,rounds,opsPerRun,reps,bytesPerOpMedian,bytesPerOpMin,bytesPerOpMax,bytesPerRoundMedian");
            for (int n : JolFootprint.NS) {
                for (String impl : JolFootprint.IMPLS) {
                    for (int k : JolFootprint.KS) {
                        S3Workload w = new S3Workload(impl, n, k);
                        double[] perOp = new double[REPS - WARMUP];
                        for (int rep = 0; rep < REPS; rep++) {
                            w.reset();
                            long before = mx.getThreadAllocatedBytes(tid);
                            blackhole += w.run();
                            long bytes = mx.getThreadAllocatedBytes(tid) - before;
                            w.check();
                            if (rep >= WARMUP) perOp[rep - WARMUP] = (double) bytes / w.opsPerRun();
                        }
                        Arrays.sort(perOp);
                        double med = median(perOp);
                        out.printf("%s,%d,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f%n", impl, n, k, w.rounds,
                                w.opsPerRun(), perOp.length, med, perOp[0], perOp[perOp.length - 1],
                                med * (k + 2));
                        out.flush();
                        System.out.printf("%-3s n=%-7d k=%-2d  %.2f B/op%n", impl, n, k, med);
                    }
                }
            }
        }
        System.out.println("AllocProbe: " + csv + " (checksum " + blackhole + ")");
    }

    private static double median(double[] sorted) {
        int m = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[m] : (sorted[m - 1] + sorted[m]) / 2;
    }
}
