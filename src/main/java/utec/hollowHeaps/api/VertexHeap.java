package utec.hollowHeaps.api;

public interface VertexHeap {

    /**
     * Inserta el vértice {@code id} con clave {@code key}.
     * @throws IndexOutOfBoundsException si {@code id < 0} o {@code id >= capacity}
     * @throws IllegalStateException si {@code id} ya fue insertado o extraído
     */
    void insert(int id, long key);

    /**
     * Reduce la clave de {@code id} a {@code newKey}. No-op si {@code newKey >= clave actual}.
     * @throws IllegalStateException si {@code id} no está actualmente en el heap
     */
    void decreaseKey(int id, long newKey);

    /**
     * Extrae y devuelve el id con clave mínima, o {@code -1} si el heap está vacío.
     */
    int  deleteMin();

    /**
     * Devuelve el id con clave mínima sin extraerlo, o {@code -1} si el heap está vacío.
     */
    int  findMin();

    /**
     * Devuelve la clave actual de {@code id}.
     * @throws IllegalStateException si {@code id} no está actualmente en el heap
     */
    long keyOf(int id);

    boolean contains(int id);
    boolean isEmpty();
    int  size();
}
