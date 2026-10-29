package utec.hollowHeaps.fibonacci;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utec.hollowHeaps.api.VertexHeap;

public class FibonacciHeap implements VertexHeap {

    // Centinela: nodeOf[id] == EXTRACTED significa que el id ya fue extraído del heap
    private static final FibonacciHeapNode EXTRACTED =
            new FibonacciHeapNode(Integer.MIN_VALUE, Long.MAX_VALUE);

    FibonacciHeapNode head;
    FibonacciHeapNode min;
    int size;

    private final int capacity;
    private final FibonacciHeapNode[] nodeOf; // id -> nodo activo (null=nunca insertado, EXTRACTED=extraído)

    private final List<FibonacciHeapNode> rootList = new ArrayList<>();
    private final FibonacciHeapNode[] slots = new FibonacciHeapNode[64];

    public FibonacciHeap(int capacity) {
        this.capacity = capacity;
        this.nodeOf = new FibonacciHeapNode[capacity];
        this.head = null;
        this.min = null;
        this.size = 0;
    }

    @Override
    public int findMin() {
        return min == null ? -1 : min.id;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return head == null;
    }

    public FibonacciHeapNode getHead() {
        return head;
    }

    // criterio IsBetter:
    // a es mejor que b si a.key < b.key; si a.key == b.key, desempata a.id < b.id
    private boolean isBetterThan(FibonacciHeapNode a, FibonacciHeapNode b) {
        if (a.key != b.key) {
            return a.key < b.key;
        }
        return a.id < b.id; // desempate por id de vértice
    }

    @Override
    public void insert(int id, long key) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        if (nodeOf[id] != null) throw new IllegalStateException("id=" + id + " ya fue insertado o extraído");

        FibonacciHeapNode newNode = new FibonacciHeapNode(id, key);
        nodeOf[id] = newNode;

        if (head == null) {
            this.head = newNode;
            this.min = newNode;
            this.size = 1;
            return;
        }
        FibonacciHeapNode tail = this.head.prev;

        newNode.prev = tail;
        newNode.next = this.head;

        this.head.prev = newNode;
        tail.next = newNode;
        this.size++;

        if (isBetterThan(newNode, this.min)) {
            this.min = newNode;
        }
    }

    @Override
    public int deleteMin() {
        if (this.head == null) return -1;

        FibonacciHeapNode minNodo = this.min;
        int minimoId = minNodo.id;
        nodeOf[minimoId] = EXTRACTED;

        subirHijosARoot(minNodo);

        if (minNodo.next == minNodo) {
            this.head = null;
            this.min = null;
        } else {
            minNodo.prev.next = minNodo.next;
            minNodo.next.prev = minNodo.prev;
            if (this.head == minNodo) {
                this.head = minNodo.next;
            }

            // Consolidar el bosque para unir árboles de igual grado y actualizar this.min
            coalesce();
        }

        minNodo.next = null;
        minNodo.prev = null;
        minNodo.parent = null;
        minNodo.child = null;
        minNodo.isMarked = false;

        this.size--;
        return minimoId;
    }

    private void coalesce() {
        if (head == null) return;

        rootList.clear();
        FibonacciHeapNode curr = this.head;
        do {
            rootList.add(curr);
            curr = curr.next;
        } while (curr != this.head);

        for (FibonacciHeapNode x : rootList) {
            int d = x.degree;

            while (slots[d] != null) {
                FibonacciHeapNode y = slots[d];
                slots[d] = null;

                // El nodo "peor" se vuelve hijo del nodo "mejor"
                if (isBetterThan(y, x)) {
                    FibonacciHeapNode temp = x;
                    x = y;
                    y = temp;
                }

                x.linkNode(y);
                d = x.degree;
            }
            slots[d] = x;
        }
        rootList.clear(); // no retener referencias a nodos entre llamadas

        this.head = null;
        this.min = null;

        for (int i = 0; i < 64; i++) {
            if (slots[i] != null) {
                FibonacciHeapNode node = slots[i];
                slots[i] = null;
                node.parent = null;

                if (this.head == null) {
                    this.head = node;
                    this.head.next = node;
                    this.head.prev = node;
                    this.min = node;
                } else {
                    FibonacciHeapNode tail = this.head.prev;
                    node.prev = tail;
                    node.next = this.head;
                    tail.next = node;
                    this.head.prev = node;

                    if (isBetterThan(node, this.min)) {
                        this.min = node;
                    }
                }
            }
        }
    }

    public void subirHijosARoot(FibonacciHeapNode node) {
        if (node == null || node.child == null) {
            return;
        }

        FibonacciHeapNode current = node.child;
        int numChildren = node.degree;

        for (int i = 0; i < numChildren; i++) {
            FibonacciHeapNode nextChild = current.next;
            subirAroot(current);
            current = nextChild;
        }
        node.child = null;
        node.degree = 0;
    }

    private void subirAroot(FibonacciHeapNode node) {
        if (this.head == null) {
            this.head = node;
            node.next = node;
            node.prev = node;
        } else {
            FibonacciHeapNode tail = this.head.prev;
            node.prev = tail;
            node.next = this.head;
            tail.next = node;
            this.head.prev = node;
        }
        node.parent = null;
        node.isMarked = false;
    }

    @Override
    public void decreaseKey(int id, long newKey) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        FibonacciHeapNode nodo = nodeOf[id];
        if (nodo == null || nodo == EXTRACTED) throw new IllegalStateException("id=" + id + " no está en el heap");
        if (newKey >= nodo.key) return;

        // cambiamos la llave
        nodo.key = newKey;

        // La llave es menor que la de su padre, debemos cortar
        FibonacciHeapNode parent = nodo.parent;

        if (parent != null && isBetterThan(nodo, parent)) {
            cut(nodo, parent);
            cascadingCut(parent);
        }
        if (isBetterThan(nodo, this.min)) {
            this.min = nodo;
        }
    }

    private void cut(FibonacciHeapNode nodo, FibonacciHeapNode parent) {
        if (nodo.next != nodo) {
            nodo.next.prev = nodo.prev;
            nodo.prev.next = nodo.next;
            if (parent.child == nodo) {
                parent.child = nodo.next;
            }
        } else {
            parent.child = null;
        }
        parent.degree--;

        subirAroot(nodo);
    }

    private void cascadingCut(FibonacciHeapNode nodo) {
        FibonacciHeapNode parent = nodo.parent;
        if (parent != null) {
            if (!nodo.isMarked) {
                nodo.isMarked = true;
            } else {
                cut(nodo, parent);
                cascadingCut(parent);
            }
        }
    }

    @Override
    public long keyOf(int id) {
        if (id < 0 || id >= capacity) throw new IndexOutOfBoundsException("id=" + id);
        FibonacciHeapNode n = nodeOf[id];
        if (n == null || n == EXTRACTED) throw new IllegalStateException("id=" + id + " no está en el heap");
        return n.key;
    }

    @Override
    public boolean contains(int id) {
        if (id < 0 || id >= capacity) return false;
        return nodeOf[id] != null && nodeOf[id] != EXTRACTED;
    }

    // Métodos para imprimir el Fibonacci Heap con Colores ANSI
    public void printHeapBySlots() {
        if (this.head == null) {
            System.out.println("El Heap está totalmente vacío.");
            return;
        }

        String RESET = "[0m";
        String AMARILLO = "[33m";
        String GRIS = "[90m";

        // Mapa para agrupar las raíces por su Grado (Slot)
        Map<Integer, List<FibonacciHeapNode>> slots = new HashMap<>();
        int maxGrade = 0;

        FibonacciHeapNode curr = this.head;
        do {
            int g = curr.degree;
            if (!slots.containsKey(g)) {
                slots.put(g, new ArrayList<>());
            }
            slots.get(g).add(curr);

            if (g > maxGrade) {
                maxGrade = g;
            }

            curr = curr.next;
        } while (curr != this.head);

        System.out.println(AMARILLO + "===== ESTADO DEL FIBONACCI HEAP (POR SLOTS) =====" + RESET);
        for (int grade = 0; grade <= maxGrade; grade++) {
            System.out.println(GRIS + "Slot " + grade + ":" + RESET);
            List<FibonacciHeapNode> nodesInSlot = slots.get(grade);

            if (nodesInSlot == null || nodesInSlot.isEmpty()) {
                System.out.println("   (vacío)");
            } else {
                int count = 1;
                for (FibonacciHeapNode node : nodesInSlot) {
                    String NARANJA = "[33m";
                    String CIAN_B = "[96m";
                    String VERDE = "[32m";
                    String AZUL = "[34m";

                    String colorNodo = (node.child == null) ? VERDE : AZUL;
                    String markedStr = "";

                    if (node.isMarked) {
                        colorNodo = NARANJA;
                        markedStr = " " + NARANJA + "[M]" + RESET;
                    }

                    String isMinStr = "";
                    if (node == this.min) {
                        isMinStr = CIAN_B + " <--- (MIN)" + RESET;
                    }

                    System.out.println("   " + count + "º nodo -> " + colorNodo + "Key: " + node.key
                            + " | id: " + node.id + RESET
                            + markedStr + isMinStr);

                    if (node.child != null) {
                        printSubtree(node.child, "      ");
                    }
                    count++;
                }
            }
        }
        System.out.println(AMARILLO + "=================================================" + RESET);
    }

    private void printSubtree(FibonacciHeapNode childNode, String indent) {
        if (childNode == null) return;

        String RESET = "[0m";
        String GRIS = "[90m";
        String NARANJA = "[33m";
        String VERDE = "[32m";
        String AZUL = "[34m";

        FibonacciHeapNode currChild = childNode;
        do {
            String colorNodo = (currChild.child == null) ? VERDE : AZUL;
            String markedStr = "";

            if (currChild.isMarked) {
                colorNodo = NARANJA;
                markedStr = " " + NARANJA + "[M]" + RESET;
            }

            System.out.print(GRIS + indent + "└── [Hijo Grado " + currChild.degree + "] " + RESET);
            System.out.println(colorNodo + "Key: " + currChild.key + " | id: " + currChild.id + RESET + markedStr);

            if (currChild.child != null) {
                printSubtree(currChild.child, indent + "    ");
            }

            currChild = currChild.next;
        } while (currChild != childNode);
    }
}
