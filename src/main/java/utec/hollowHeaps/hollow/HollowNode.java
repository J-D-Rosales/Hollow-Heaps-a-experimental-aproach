package utec.hollowHeaps.hollow;

public class HollowNode {

    long key;
    int id;  // -1 => nodo HOLLOW
    int rank;
    HollowNode child;
    HollowNode next;
    HollowNode ep;

    public HollowNode(long key, int id) {
        this.key = key;
        this.id = id;
        this.rank = 0;
        this.child = null;
        this.next = null;
        this.ep = null;
    }
}
