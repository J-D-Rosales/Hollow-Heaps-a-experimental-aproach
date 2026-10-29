package utec.hollowHeaps.binary;

import java.util.Arrays;

import utec.hollowHeaps.api.VertexHeap;

/**
 * Montículo binario en arreglo con índice de posiciones (decreaseKey real).
 * Sin objetos por elemento: long[] keys (por posición), int[] heap (posición->id),
 * int[] pos (id->posición). Estado por id codificado en pos: -2=nunca, -1=extraído, >=0=en heap.
 */
public class IndexedBinaryHeap implements VertexHeap {

    private final int capacity;
    private final long[] keys; // clave en la posición i del heap
    private final int[] heap;  // id en la posición i del heap
    private final int[] pos;   // posición del id en el heap (-2=nunca, -1=extraído, >=0=en heap)
    private int size;

    public IndexedBinaryHeap(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity debe ser >= 1");
        this.capacity = capacity;
        this.keys = new long[capacity];
        this.heap = new int[capacity];
        this.pos = new int[capacity];
        Arrays.fill(this.pos, -2);
        this.size = 0;
    }

    @Override
    public void insert(int id, long key) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        if (pos[id] != -2) throw new IllegalStateException("id=" + id + " ya fue insertado");
        heap[size] = id;
        keys[size] = key;
        pos[id] = size;
        size++;
        siftUp(size - 1);
    }

    @Override
    public int findMin() {
        return size == 0 ? -1 : heap[0];
    }

    @Override
    public int deleteMin() {
        if (size == 0) return -1;
        int minId = heap[0];
        pos[minId] = -1; // extraído
        size--;
        if (size > 0) {
            heap[0] = heap[size];
            keys[0] = keys[size];
            pos[heap[0]] = 0;
            siftDown(0);
        }
        return minId;
    }

    @Override
    public void decreaseKey(int id, long newKey) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        if (pos[id] < 0) throw new IllegalStateException("id=" + id + " no está en el heap");
        int i = pos[id];
        if (newKey >= keys[i]) return;
        keys[i] = newKey;
        siftUp(i);
    }

    @Override
    public long keyOf(int id) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        if (pos[id] < 0) throw new IllegalStateException("id=" + id + " no está en el heap");
        return keys[pos[id]];
    }

    @Override
    public boolean contains(int id) {
        return id >= 0 && id < capacity && pos[id] >= 0;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    // Sift con "hueco": el elemento se mueve una sola vez al encontrar su posición final
    private void siftUp(int i) {
        int id = heap[i];
        long k = keys[i];
        while (i > 0) {
            int p = (i - 1) >>> 1;
            if (keys[p] <= k) {
                break;
            }
            heap[i] = heap[p];
            keys[i] = keys[p];
            pos[heap[i]] = i;
            i = p;
        }
        heap[i] = id;
        keys[i] = k;
        pos[id] = i;
    }

    private void siftDown(int i) {
        int id = heap[i];
        long k = keys[i];
        int half = size >>> 1; // los índices >= half son hojas
        while (i < half) {
            int c = 2 * i + 1;
            int r = c + 1;
            if (r < size && keys[r] < keys[c]) {
                c = r;
            }
            if (k <= keys[c]) { // corte en empates
                break;
            }
            heap[i] = heap[c];
            keys[i] = keys[c];
            pos[heap[i]] = i;
            i = c;
        }
        heap[i] = id;
        keys[i] = k;
        pos[id] = i;
    }
}
