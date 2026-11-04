package utec.hollowHeaps.fibonacci;

public class FibonacciHeapNode {
    int id;
    long key;
    int degree;

    FibonacciHeapNode next;
    FibonacciHeapNode prev;
    FibonacciHeapNode parent;
    FibonacciHeapNode child;

    boolean isMarked;

    public FibonacciHeapNode(int id, long key) {
        this.id = id;
        this.key = key;
        this.degree = 0;
        this.isMarked = false;
        this.next = this;
        this.prev = this;
        this.parent = null;
        this.child = null;
    }

    void linkNode(FibonacciHeapNode node) {
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
