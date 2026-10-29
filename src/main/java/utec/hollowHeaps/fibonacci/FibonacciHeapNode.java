package utec.hollowHeaps.fibonacci;

public class FibonacciHeapNode {
    public int id;
    public long key;
    public int degree;

    public FibonacciHeapNode next;
    public FibonacciHeapNode prev;
    public FibonacciHeapNode parent;
    public FibonacciHeapNode child;

    public boolean isMarked;

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
