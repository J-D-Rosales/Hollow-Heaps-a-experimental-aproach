package utec.hollowHeaps.hollowHeap;

public class HollowItem {
    // Puntero referencial
    public HollowNode node;
    public String value; // Juanito por ejemplo

    public HollowItem(HollowNode node, String value) {
        this.node = node;
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
