package utec.hollowHeaps.hollowHeap;

public class HollowNode {

    public HollowItem item;
    public HollowNode child;
    public HollowNode next;
    public HollowNode ep;

    public int rank;
    public int key;

    public HollowNode(int key, String item){
        this.item = new HollowItem(this, item);
        this.key = key;
        this.child = null;
        this.next = null;
        this.ep = null;
        this.rank = 0;
    }

    // Constructor para DECREASE-KEY (reutiliza el ítem existente)
    public HollowNode(int key, HollowItem item) {
        this.item = item;     // Reutiliza la misma instancia de la memoria
        this.key = key;
        this.child = null;
        this.next = null;
        this.ep = null;
        this.rank = 0;
    }
}
