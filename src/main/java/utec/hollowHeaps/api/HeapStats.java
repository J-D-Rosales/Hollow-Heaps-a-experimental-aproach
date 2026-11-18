package utec.hollowHeaps.api;

/**
 * Contadores de operaciones de una estructura (D5). Solo se actualizan con
 * {@code -Dheap.instrument=true}; se leen con {@code stats()}.
 *
 * <p>NOTA: versión mínima reconstruida porque el archivo original no estaba en el repo.
 * Contiene exactamente los campos que usan HH, FH, IBH y PQL. Si existe una versión de
 * Fase 1, reemplazar esta.
 */
public final class HeapStats {
    // comunes
    public long inserts, decreaseKeys, decreaseKeyNoOps, deleteMins, maxSize;
    // HH y FH
    public long links, maxNodes;
    // HH
    public long hollowCreated, hollowFreed;
    // FH
    public long cuts, cascadingCuts;
    // IBH
    public long siftSteps;
    // PQL
    public long staleDiscarded, maxPhysicalSize;

    @Override
    public String toString() {
        return "HeapStats{inserts=" + inserts + ", decreaseKeys=" + decreaseKeys
                + ", decreaseKeyNoOps=" + decreaseKeyNoOps + ", deleteMins=" + deleteMins
                + ", maxSize=" + maxSize + ", links=" + links + ", maxNodes=" + maxNodes
                + ", hollowCreated=" + hollowCreated + ", hollowFreed=" + hollowFreed
                + ", cuts=" + cuts + ", cascadingCuts=" + cascadingCuts
                + ", siftSteps=" + siftSteps + ", staleDiscarded=" + staleDiscarded
                + ", maxPhysicalSize=" + maxPhysicalSize + "}";
    }
}
