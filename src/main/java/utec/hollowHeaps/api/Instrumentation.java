package utec.hollowHeaps.api;

/**
 * Bandera global de instrumentación (D5). Se activa con {@code -Dheap.instrument=true}.
 *
 * <p>Es {@code static final}: con la bandera apagada, el JIT elimina los bloques
 * {@code if (Instrumentation.ENABLED)}, así que los benchmarks de tiempo no pagan los contadores.
 *
 * <p>NOTA: versión mínima reconstruida porque el archivo original no estaba en el repo
 * (las estructuras ya lo referenciaban). Si existe una versión de Fase 1, reemplazar esta.
 */
public final class Instrumentation {
    public static final boolean ENABLED = Boolean.getBoolean("heap.instrument");

    private Instrumentation() {}
}
