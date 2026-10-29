package utec.hollowHeaps.adapter;

import java.util.Comparator;
import java.util.PriorityQueue;

import utec.hollowHeaps.api.VertexHeap;

/**
 * java.util.PriorityQueue con lazy insertion: decreaseKey inserta una entrada nueva
 * y las entradas obsoletas se descartan cuando llegan a la cima.
 * La cola física puede tener más entradas que elementos vivos (ver physicalSize()).
 */
public class JavaPriorityQueueAdapter implements VertexHeap {

    private record Entry(long key, int id) {}

    // D4: PriorityQueue sin ajustes (capacidad por defecto)
    private final PriorityQueue<Entry> pq = new PriorityQueue<>(Comparator.comparingLong(Entry::key));

    private final int capacity;
    private final long[] dist;   // id -> clave actual (válido solo si state[id]==1)
    private final byte[] state;  // 0=nunca insertado, 1=en heap, 2=extraído
    private int size;            // elementos vivos, no entradas físicas

    public JavaPriorityQueueAdapter(int capacity) {
        this.capacity = capacity;
        this.dist = new long[capacity];
        this.state = new byte[capacity];
    }

    @Override
    public void insert(int id, long key) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        if (state[id] != 0) throw new IllegalStateException("id=" + id + " ya fue insertado o extraído");
        dist[id] = key;
        state[id] = 1;
        pq.add(new Entry(key, id));
        size++;
    }

    @Override
    public int findMin() {
        Entry top = purgeStale();
        return top == null ? -1 : top.id();
    }

    @Override
    public int deleteMin() {
        Entry top = purgeStale();
        if (top == null) return -1;
        pq.poll();
        int id = top.id();
        state[id] = 2;
        size--;
        return id;
    }

    @Override
    public void decreaseKey(int id, long newKey) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        if (state[id] != 1) throw new IllegalStateException("id=" + id + " no está en el heap");
        if (newKey >= dist[id]) return;
        dist[id] = newKey;
        pq.add(new Entry(newKey, id)); // la entrada anterior queda obsoleta
    }

    @Override
    public long keyOf(int id) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        if (state[id] != 1) throw new IllegalStateException("id=" + id + " no está en el heap");
        return dist[id];
    }

    @Override
    public boolean contains(int id) {
        return id >= 0 && id < capacity && state[id] == 1;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    // Entradas físicas en la cola (vivas + obsoletas). Para medir H2b
    public int physicalSize() {
        return pq.size();
    }

    // findMin también limpia la cima: tiene efecto lateral sobre la cola física
    private Entry purgeStale() {
        Entry top;
        while ((top = pq.peek()) != null && isStale(top)) {
            pq.poll();
        }
        return top;
    }

    // Una entrada es obsoleta si el id ya no está en el heap o si su clave no coincide con dist
    private boolean isStale(Entry e) {
        return state[e.id()] != 1 || e.key() != dist[e.id()];
    }
}
