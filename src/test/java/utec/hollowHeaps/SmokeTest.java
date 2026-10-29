package utec.hollowHeaps;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import utec.hollowHeaps.adapter.JavaPriorityQueueAdapter;
import utec.hollowHeaps.api.VertexHeap;
import utec.hollowHeaps.binary.IndexedBinaryHeap;
import utec.hollowHeaps.fibonacci.FibonacciHeap;
import utec.hollowHeaps.hollow.HollowHeap;

class SmokeTest {

    private static final int CAP = 10_000;
    private static final int OPS = 10_000;

    @Test void hollowHeapSmoke()    { runSmoke(new HollowHeap(CAP)); }
    @Test void fibonacciHeapSmoke() { runSmoke(new FibonacciHeap(CAP)); }
    @Test void indexedBinarySmoke() { runSmoke(new IndexedBinaryHeap(CAP)); }
    @Test void javaPQSmoke()        { runSmoke(new JavaPriorityQueueAdapter(CAP)); }

    @Test
    void contrato() {
        for (VertexHeap h : new VertexHeap[]{
                new HollowHeap(10),
                new FibonacciHeap(10),
                new IndexedBinaryHeap(10),
                new JavaPriorityQueueAdapter(10)}) {
            contratoTest(h);
        }
    }

    // -------------------------------------------------------------------------
    // Test de contrato: invariantes independientes del algoritmo
    // -------------------------------------------------------------------------
    private void contratoTest(VertexHeap h) {
        String n = h.getClass().getSimpleName();

        // Vacío
        assertEquals(-1, h.findMin(),   n + ": findMin vacío");
        assertEquals(-1, h.deleteMin(), n + ": deleteMin vacío");
        assertTrue(h.isEmpty(), n + ": isEmpty en vacío");
        assertEquals(0, h.size(), n + ": size en vacío");

        // Insert y consultas básicas
        h.insert(3, 100L);
        assertTrue(h.contains(3),         n + ": contains tras insert");
        assertEquals(100L, h.keyOf(3),    n + ": keyOf tras insert");
        assertEquals(3, h.findMin(),      n + ": findMin con un solo elemento");
        assertEquals(1, h.size(),         n + ": size=1");
        assertFalse(h.isEmpty(),          n + ": no vacío");

        // decreaseKey no-op (igual o mayor)
        h.decreaseKey(3, 100L);
        h.decreaseKey(3, 200L);
        assertEquals(100L, h.keyOf(3),    n + ": decreaseKey no-op no cambia clave");

        // decreaseKey válido
        h.decreaseKey(3, 50L);
        assertEquals(50L, h.keyOf(3),     n + ": decreaseKey válido");

        // Insertar otro y verificar mínimo
        h.insert(7, 10L);
        assertEquals(7, h.findMin(),      n + ": findMin con dos elementos");

        // Extraer mínimo
        assertEquals(7, h.deleteMin(),    n + ": deleteMin extrae el menor (id=7, key=10)");
        assertFalse(h.contains(7),        n + ": contains false tras extraer");
        assertThrows(IllegalStateException.class, () -> h.keyOf(7),
                n + ": keyOf sobre id extraído");

        // Reinsertar un id extraído → IllegalStateException
        assertThrows(IllegalStateException.class, () -> h.insert(7, 999L),
                n + ": reinsertar id extraído");

        // decreaseKey sobre id extraído → IllegalStateException
        assertThrows(IllegalStateException.class, () -> h.decreaseKey(7, 1L),
                n + ": decreaseKey sobre extraído");

        // decreaseKey sobre id nunca insertado → IllegalStateException
        assertThrows(IllegalStateException.class, () -> h.decreaseKey(5, 1L),
                n + ": decreaseKey sobre nunca insertado");

        // keyOf sobre id nunca insertado → IllegalStateException
        assertThrows(IllegalStateException.class, () -> h.keyOf(5),
                n + ": keyOf sobre nunca insertado");

        // insert fuera de rango → IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> h.insert(-1, 1L),
                n + ": insert id=-1");
        assertThrows(IndexOutOfBoundsException.class, () -> h.insert(10, 1L),
                n + ": insert id=capacity");

        // deleteMin sobre heap con un elemento pendiente (id=3)
        assertEquals(3, h.deleteMin(), n + ": drain final");
        assertEquals(-1, h.deleteMin(), n + ": deleteMin sobre vacío final");
    }

    // -------------------------------------------------------------------------
    // Smoke test principal: 10 000 operaciones mixtas + drain final
    // -------------------------------------------------------------------------
    private void runSmoke(VertexHeap heap) {
        String name = heap.getClass().getSimpleName();
        Random rng = new Random(42);

        // Oráculo: multiset de claves activas
        PriorityQueue<Long> oracle = new PriorityQueue<>();

        long[] currentKey = new long[CAP];
        List<Integer> availList  = new ArrayList<>(CAP);
        List<Integer> inHeapList = new ArrayList<>(CAP);
        for (int i = 0; i < CAP; i++) availList.add(i);

        for (int op = 0; op < OPS; op++) {
            int r = rng.nextInt(10); // 0-9 → 0-3=insert(40%), 4-7=dk(40%), 8-9=deleteMin(20%)

            if (r < 4 && !availList.isEmpty()) {
                // INSERT
                int idx = rng.nextInt(availList.size());
                int id  = availList.remove(idx);
                long key = rng.nextLong(0L, 1_000_000_000_000L);
                heap.insert(id, key);
                oracle.add(key);
                currentKey[id] = key;
                inHeapList.add(id);

            } else if (r < 8 && !inHeapList.isEmpty()) {
                // DECREASEKEY sobre un id vivo elegido al azar
                int idx = rng.nextInt(inHeapList.size());
                int id  = inHeapList.get(idx);
                long delta  = rng.nextLong(1L, 100_000L);
                long newKey = currentKey[id] - delta;
                oracle.remove(currentKey[id]); // O(n), aceptable en smoke test
                oracle.add(newKey);
                heap.decreaseKey(id, newKey);
                currentKey[id] = newKey;

            } else if (!inHeapList.isEmpty()) {
                // DELETEMIN
                long expectedKey = oracle.poll();
                int  extractedId = heap.deleteMin();
                assertNotEquals(-1, extractedId,
                        name + ": deleteMin devolvió -1 en op " + op);
                assertEquals(expectedKey, currentKey[extractedId],
                        name + ": clave incorrecta en op " + op
                        + " (id=" + extractedId + ")");
                inHeapList.remove((Integer) extractedId);
            }
        }

        // Vaciar el heap y comparar la secuencia restante
        while (!inHeapList.isEmpty()) {
            long expectedKey = oracle.poll();
            int  extractedId = heap.deleteMin();
            assertNotEquals(-1, extractedId, name + ": drain: deleteMin devolvió -1");
            assertEquals(expectedKey, currentKey[extractedId],
                    name + ": clave incorrecta en drain (id=" + extractedId + ")");
            inHeapList.remove((Integer) extractedId);
        }

        // Heap vacío: deleteMin y findMin deben devolver -1
        assertEquals(-1, heap.deleteMin(), name + ": deleteMin sobre vacío");
        assertEquals(-1, heap.findMin(),   name + ": findMin sobre vacío");
        assertTrue(heap.isEmpty(),         name + ": isEmpty al final");
        assertEquals(0, heap.size(),       name + ": size=0 al final");
    }
}
