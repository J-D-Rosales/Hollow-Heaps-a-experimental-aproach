package utec.hollowHeaps.api;

public interface VertexHeap {
    void insert(int id, long key);
    void decreaseKey(int id, long newKey);
    int  deleteMin();
    int  findMin();
    long keyOf(int id);
    boolean contains(int id);
    boolean isEmpty();
    int  size();
}
