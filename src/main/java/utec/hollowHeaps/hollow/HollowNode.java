package utec.hollowHeaps.hollow;

public class HollowNode {

    public long key;
    public int id;  // -1 => nodo HOLLOW
    public int rank;
    public HollowNode child;
    public HollowNode next;
    public HollowNode ep;

    public HollowNode(long key, int id) {
        this.key = key;
        this.id = id;
        this.rank = 0;
        this.child = null;
        this.next = null;
        this.ep = null;
    }
}
