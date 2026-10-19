package utec.hollowHeaps.fibonacciHeap;

public class FibonacciHeapNode {
    public int id; // for min value tied nodes, we need to tie-break them and choose a winner
    String value;
    Integer key;
    int degree;

    FibonacciHeapNode next;
    FibonacciHeapNode prev;

    FibonacciHeapNode parent;
    FibonacciHeapNode child;

    boolean isMarked;

    // Constructor
    public FibonacciHeapNode(int id,Integer key, String value) {
        this.id = id;
        this.value = value;
        this.key = key;
        this.degree = 0;
        this.isMarked = false;
        this.next = this;
        this.prev = this;
        this.parent = null;
        this.child = null;
    }

    // Convierte a 'node' en hijo de 'this'

    public void linkNode(FibonacciHeapNode node) {
        if (node == null) return;
        node.parent = this;
        node.isMarked = false;

        if (this.child == null) {
            this.child = node;
            node.next = node;
            node.prev = node;
        } else {
            FibonacciHeapNode tail = this.child.prev;

            node.prev = tail;
            node.next = this.child;
            tail.next = node;
            this.child.prev = node;
        }

        this.degree++;
    }

}
