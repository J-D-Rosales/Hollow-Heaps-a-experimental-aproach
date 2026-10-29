package utec.hollowHeaps.hollow;

import java.util.ArrayList;
import java.util.List;

import utec.hollowHeaps.api.VertexHeap;

public class HollowHeap implements VertexHeap {

    // Centinela: nodeOf[id] == EXTRACTED significa que el id ya fue extraído del heap
    private static final HollowNode EXTRACTED = new HollowNode(Long.MAX_VALUE, Integer.MIN_VALUE);

    HollowNode root;
    int size;

    private final int capacity;
    private final HollowNode[] nodeOf; // id -> nodo activo actual (null=nunca insertado, EXTRACTED=extraído)

    // D4: buffers de deleteMin reutilizados entre llamadas (misma política que FibonacciHeap)
    private final List<HollowNode> L = new ArrayList<>();
    private final HollowNode[] A = new HollowNode[64]; // rank <= log_phi(n) ~ 45 para n <= 2^31

    public HollowHeap(int capacity) {
        this.capacity = capacity;
        this.nodeOf = new HollowNode[capacity];
        buildHeap();
    }

    private void buildHeap() {
        this.root = null;
        this.size = 0;
    }

    private HollowNode link(HollowNode u, HollowNode v) {
        if (u == null) return v;
        if (v == null) return u;

        // Ambos nodos son roots
        if (u.key > v.key) {
            HollowNode aux = v;
            v = u;
            u = aux;
        }
        v.next = u.child;
        u.child = v;

        return u;
    }

    @Override
    public int findMin() {
        return root == null ? -1 : root.id;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public void insert(int id, long key) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        if (nodeOf[id] != null) throw new IllegalStateException("id=" + id + " ya fue insertado o extraído");

        HollowNode nuevoNodo = new HollowNode(key, id);
        nodeOf[id] = nuevoNodo;

        if (this.root == null) {
            this.root = nuevoNodo;
        } else {
            this.root = link(this.root, nuevoNodo);
        }
        this.size++;
    }

    @Override
    public void decreaseKey(int id, long newKey) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        HollowNode u = nodeOf[id];
        if (u == null || u == EXTRACTED) throw new IllegalStateException("id=" + id + " no está en el heap");
        if (newKey >= u.key) return;

        // Caso de si u es root
        if (u == this.root) {
            u.key = newKey;
            return;
        }

        // Caso general: nuevo nodo v con la clave reducida, u pasa a ser Hollow
        HollowNode v = new HollowNode(newKey, id);
        v.rank = Math.max(0, u.rank - 2);
        nodeOf[id] = v;

        u.id = -1; // u pasa a ser hollow
        v.child = u; // su unico hijo
        u.ep = v;    // si no es el root asignamos un segundo padre

        this.root = link(this.root, v);
    }

    @Override
    public int deleteMin() {
        if (this.root == null) return -1;

        // Vaciamos la raíz
        int minimoId = this.root.id;
        this.root.id = -1; // root pasa a hollow
        nodeOf[minimoId] = EXTRACTED;
        this.size--;

        // Reestructuracion del heap y eliminación de los Hollow
        L.add(this.root);
        int maxRank = 0;

        while (!L.isEmpty()) {
            HollowNode w = L.removeLast();
            HollowNode c = w.child;
            w.child = null; // W queda solo

            while (c != null) {
                HollowNode nextC = c.next;

                if (c.id == -1) { // hollow
                    if (c.ep != null) {
                        if (c.ep == w) {
                            nextC = null; // Si w era su segundo padre, era el último hijo
                        } else {
                            c.next = null;
                        }
                        c.ep = null;
                    } else {
                        L.add(c);
                    }
                } else { // C está activo: colocarlo en A y hacer coalesce
                    c.next = null;

                    while (A[c.rank] != null) {
                        HollowNode otro = A[c.rank];
                        A[c.rank] = null;
                        c = link(c, otro);
                        c.rank = c.rank + 1;
                    }
                    A[c.rank] = c;
                    maxRank = Math.max(maxRank, c.rank);
                }
                c = nextC;
            }
        }

        // Reconstruir la raiz global uniendo los árboles en A
        this.root = null;
        for (int i = 0; i <= maxRank; i++) {
            if (A[i] != null) {
                HollowNode t = A[i];
                A[i] = null; // dejar A limpio para el siguiente deleteMin
                if (this.root == null) {
                    this.root = t;
                } else {
                    this.root = link(this.root, t);
                }
            }
        }
        return minimoId;
    }

    @Override
    public long keyOf(int id) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        HollowNode n = nodeOf[id];
        if (n == null || n == EXTRACTED) throw new IllegalStateException("id=" + id + " no está en el heap");
        return n.key;
    }

    @Override
    public boolean contains(int id) {
        if (id < 0 || id >= capacity) return false;
        return nodeOf[id] != null && nodeOf[id] != EXTRACTED;
    }

    public void printHeap() {
        String RESET = "[0m";
        String AMARILLO = "[33m";

        System.out.println(AMARILLO + "================ ESTADO DEL HOLLOW HEAP ================" + RESET);
        if (this.root == null) {
            System.out.println("(Heap vacío)");
        } else {
            printNode(this.root, "", true);
        }
        System.out.println(AMARILLO + "========================================================" + RESET);
    }

    private void printNode(HollowNode node, String indent, boolean isLast) {
        if (node == null) return;

        String RESET = "[0m";
        String ROJO = "[38;5;208m";
        String VERDE = "[32m";
        String AZUL = "[34m";
        String GRIS = "[90m";

        System.out.print(GRIS + indent + (isLast ? "└── " : "├── ") + RESET);
        if (isLast) {
            indent += "    ";
        } else {
            indent += "│   ";
        }

        String colorNodo;
        String estado;

        if (node.id == -1) {
            colorNodo = ROJO;
            estado = "HOLLOW";
        } else if (node.child == null) {
            colorNodo = VERDE;
            estado = "HOJA [id: " + node.id + "]";
        } else {
            colorNodo = AZUL;
            estado = "FULL [id: " + node.id + "]";
        }

        String extraParent = (node.ep != null) ? " (EP -> Key: " + node.ep.key + ")" : "";
        System.out.println(colorNodo + estado + " (Key: " + node.key + ", Rank: " + node.rank + ")" + extraParent + RESET);

        int childCount = 0;
        HollowNode curr = node.child;
        while (curr != null) {
            childCount++;
            curr = curr.next;
        }

        curr = node.child;
        int index = 0;
        while (curr != null) {
            index++;
            boolean lastChild = (index == childCount);
            printNode(curr, indent, lastChild);
            curr = curr.next;
        }
    }
}
