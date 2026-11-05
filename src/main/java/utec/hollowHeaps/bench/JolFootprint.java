package utec.hollowHeaps.bench;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import org.openjdk.jol.info.GraphLayout;

import com.sun.management.HotSpotDiagnosticMXBean;

import utec.hollowHeaps.api.VertexHeap;

/**
 * Medición JOL (H3a): bytes retenidos por la estructura, con GraphLayout sobre todo lo
 * alcanzable desde el heap (nodos, nodos hollow vivos, entradas de PQL, nodeOf[]/pos[]/dist[],
 * buffers).
 *
 * <ul>
 *   <li>Escenario S1: tras n inserts (sin decreaseKey ni deleteMin), capacidad = n.</li>
 *   <li>Escenario S3: estado al final de una invocación de S3Mixed (misma {@link S3Workload}),
 *       para cada k. Capacidad = n + ROUNDS + 1.</li>
 * </ul>
 *
 * <p>Columnas:
 * <ul>
 *   <li>totalBytes / bytesPerElement: todo, dividido entre n (elementos vivos);</li>
 *   <li>objectBytes / objectBytesPerElement: solo objetos que no son arreglos (nodos HH/FH,
 *       Entry de PQL). No depende de la capacidad: es la cifra para comparar H3a según k;</li>
 *   <li>arrayBytes: arreglos (nodeOf[], keys[], pos[], cola interna de PQL...). En S3 crece con
 *       la capacidad n + ROUNDS, no con k;</li>
 *   <li>nodeCount: nodos de la estructura (HollowNode incluye los hollow; Entry de PQL incluye
 *       las obsoletas).</li>
 * </ul>
 *
 * <pre>
 * java -Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading -Djol.magicFieldOffset=true \
 *      -Xms4g -Xmx4g -cp target/benchmarks.jar utec.hollowHeaps.bench.JolFootprint results/jol.csv
 * </pre>
 * Correr una vez con CompressedOops (por defecto) y otra con -XX:-UseCompressedOops.
 */
public final class JolFootprint {

    static final String[] IMPLS = {"HH", "FH", "IBH", "PQL"};
    /** Tamaños n; se pueden reducir con -Dbench.ns=10000 (modo smoke). */
    static final int[] NS = java.util.Arrays.stream(
            System.getProperty("bench.ns", "10000,1000000").split(","))
            .mapToInt(s -> Integer.parseInt(s.trim())).toArray();
    static final int[] KS = {0, 1, 4, 16, 64};

    public static void main(String[] args) throws IOException {
        Path csv = Path.of(args.length > 0 ? args[0] : "results/jol.csv");
        Path detail = Path.of(csv.toString().replaceAll("\\.csv$", "") + "-footprint.txt");
        if (csv.getParent() != null) Files.createDirectories(csv.getParent());
        String coops = vmOption("UseCompressedOops");

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(csv));
             PrintWriter det = new PrintWriter(Files.newBufferedWriter(detail))) {
            out.println("scenario,impl,n,k,rounds,capacity,compressedOops,totalBytes,bytesPerElement,"
                    + "objectBytes,objectBytesPerElement,arrayBytes,nodeCount,objectCount");
            for (int n : NS) {
                for (String impl : IMPLS) {
                    record(out, det, "S1", impl, n, 0, 0, n, coops, buildS1(impl, n));
                    for (int k : KS) {
                        S3Workload w = new S3Workload(impl, n, k);
                        w.reset();
                        w.run();
                        w.check();
                        record(out, det, "S3", impl, n, k, w.rounds, n + w.rounds + 1, coops, w.heap());
                    }
                }
            }
        }
        System.out.println("JOL: " + csv + " y " + detail);
    }

    private static void record(PrintWriter out, PrintWriter det, String scenario, String impl, int n,
                               int k, int rounds, int capacity, String coops, VertexHeap h) {
        if (h.size() != n) throw new IllegalStateException(impl + ": size " + h.size() + " != " + n);
        GraphLayout g = GraphLayout.parseInstance(h);
        long total = g.totalSize(), arrays = 0, nodes = 0;
        for (Class<?> c : g.getClasses()) {
            if (c.isArray()) arrays += g.getClassSizes().count(c);
            String name = c.getName();
            if (name.endsWith("HollowNode") || name.endsWith("FibonacciHeapNode") || name.endsWith("$Entry")) {
                nodes += g.getClassCounts().count(c);
            }
        }
        long objects = total - arrays;
        out.printf("%s,%s,%d,%d,%d,%d,%s,%d,%.3f,%d,%.3f,%d,%d,%d%n", scenario, impl, n, k, rounds,
                capacity, coops, total, (double) total / n, objects, (double) objects / n, arrays,
                nodes, g.totalCount());
        out.flush();
        det.printf("=== %s impl=%s n=%d k=%d capacity=%d compressedOops=%s ===%n%s%n",
                scenario, impl, n, k, capacity, coops, g.toFootprint());
        det.flush();
        System.out.printf("%s %-3s n=%-7d k=%-2d  total %.1f B/elem, objetos %.1f B/elem, nodos %d%n",
                scenario, impl, n, k, (double) total / n, (double) objects / n, nodes);
    }

    /** Igual que el setup de S1Insert + insertAll. */
    static VertexHeap buildS1(String impl, int n) {
        long[] keys = Heaps.randomKeys(n, 42L);
        VertexHeap h = Heaps.create(impl, n);
        for (int i = 0; i < n; i++) h.insert(i, keys[i]);
        return h;
    }

    private static String vmOption(String name) {
        try {
            return ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
                    .getVMOption(name).getValue();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}
