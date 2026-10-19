package utec.hollowHeaps.fibonacciHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FibonacciHeap {
    FibonacciHeapNode head;
    FibonacciHeapNode min;
    int size;

    public FibonacciHeap() { // constuctor
        this.head = null;
        this.min = null;
        this.size = 0;
    }

    // criterio IsBetter: 
        // principle: Alignig to theory: node a is better than b as long as a.key < b.key, if a.key == b.key, then we use a.id < b.id to break the tie
        // remember: we are in a minheap. We want to have in the root the "minimum" node
    private boolean isBetterThan( FibonacciHeapNode a, FibonacciHeapNode b){
        
        if (a.key != b.key){ // what if use < directly to compare,  instead of using != d
            return a.key < b.key;
        }
         
        return a.id < b.id; // tie-breaker
    }

    public FibonacciHeapNode enqueue(int id, Integer key,String value) {
        FibonacciHeapNode newNode = new FibonacciHeapNode(id, key, value);
        if (head == null) {
            this.head = newNode;
            this.min = newNode;
            this.size = 1;
            return newNode;
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

        return newNode;
    }

    public FibonacciHeapNode extractMin() {
        if (this.head == null) {
            return null;
        }

        FibonacciHeapNode min = this.min;
        subirHijosARoot(min);

        if (min.next == min) {
            this.head = null;
            this.min = null;
        } else {
            min.prev.next = min.next;
            min.next.prev = min.prev;
            if (this.head == min) {
                this.head = min.next;
            }

            // 3. Consolidar el bosque para unir árboles de igual grado y actualizar this.min
            coalesce();
        }

        min.next = null;
        min.prev = null;
        min.parent = null;
        min.child = null;
        min.isMarked = false;

        this.size--;
        return min;
    }

    private void coalesce() { // proposito de coalesce es : 
        if (head == null) return;

        List<FibonacciHeapNode> rootList = new ArrayList<>();
        FibonacciHeapNode curr = this.head;
        do {
            rootList.add(curr);
            curr = curr.next;
        } while (curr != this.head);

        FibonacciHeapNode[] slots = new FibonacciHeapNode[64];

        for (FibonacciHeapNode x : rootList) {
            int d = x.degree;

            while (slots[d] != null) {
                FibonacciHeapNode y = slots[d];
                slots[d] = null;

                // El nodo "peor" (clave mayor o id mayor) se vuelve hijo del nodo "mejor"
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

        this.head = null;
        this.min = null;

        for (int i = 0; i < 64; i++) {
            if (slots[i] != null) {
                FibonacciHeapNode node = slots[i];
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

    private void subirAroot(FibonacciHeapNode node){
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

    public void decreaseKey(FibonacciHeapNode nodo, Integer nuevoKey){
        // verificaciones
        if (nodo == null || nuevoKey == null){
            return;
        }
        if (nuevoKey >= nodo.key){
            return;
        }

        // cambiamos la llave
        nodo.key = nuevoKey;

        // La llave es menor que la de su padre, debemos actualizarla
        FibonacciHeapNode parent = nodo.parent;

        if (parent != null && isBetterThan(nodo, parent)) {
            cut(nodo, parent);
            cascadingCut(parent);
        }
        if (this.min == null || nodo.key < this.min.key) {
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


    // Métodos para imprimir el Fibonacci Heap con Colores ANSI
    public void printHeapBySlots() {
        if (this.head == null) {
            System.out.println("El Heap está totalmente vacío.");
            return;
        }

        // Definición de colores ANSI básicos
        String RESET = "\u001B[0m";
        String AMARILLO = "\u001B[33m";
        String GRIS = "\u001B[90m";

        // Mapa para agrupar las raíces por su Grado (Slot)
        Map<Integer, List<FibonacciHeapNode>> slots = new HashMap<>();
        int maxGrade = 0;

        // 1. Recorrer la lista circular de raíces principales
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

        // 2. Imprimir slot por slot desde 0 hasta el grado máximo encontrado
        System.out.println(AMARILLO + "===== ESTADO DEL FIBONACCI HEAP (POR SLOTS) =====" + RESET);
        for (int grade = 0; grade <= maxGrade; grade++) {
            System.out.println(GRIS + "Slot " + grade + ":" + RESET);
            List<FibonacciHeapNode> nodesInSlot = slots.get(grade);

            if (nodesInSlot == null || nodesInSlot.isEmpty()) {
                System.out.println("   (vacío)");
            } else {
                int count = 1;
                for (FibonacciHeapNode node : nodesInSlot) {
                    // Configurar colores específicos para este nodo
                    String NARANJA = "\u001B[33m"; // Naranja/Marrón para marcados
                    String CIAN_B = "\u001B[96m";   // Cian brillante para el mínimo
                    String VERDE = "\u001B[32m";    // Verde para hojas
                    String AZUL = "\u001B[34m";     // Azul para nodos raíz/internos estándar

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

                    // Imprimir el nodo principal del slot
                    System.out.println("   " + count + "º nodo -> " + colorNodo + "Key: " + node.key
                            + " | Value: \"" + node.value + "\"" + RESET
                            + markedStr + isMinStr);

                    // Mostrar subárbol si el nodo tiene hijos
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

        String RESET = "\u001B[0m";
        String GRIS = "\u001B[90m";
        String NARANJA = "\u001B[33m";
        String VERDE = "\u001B[32m";
        String AZUL = "\u001B[34m";

        // Recorrido de lista CIRCULAR de hijos (evita bucles infinitos)
        FibonacciHeapNode currChild = childNode;
        do {
            String colorNodo = (currChild.child == null) ? VERDE : AZUL;
            String markedStr = "";

            if (currChild.isMarked) {
                colorNodo = NARANJA;
                markedStr = " " + NARANJA + "[M]" + RESET;
            }

            // Prefijo estructurado en Gris para que se vea limpio
            System.out.print(GRIS + indent + "└── [Hijo Grado " + currChild.degree + "] " + RESET);

            // Contenido del nodo con su respectivo color
            System.out.println(colorNodo + "Key: " + currChild.key + " | Value: \"" + currChild.value + "\"" + RESET + markedStr);

            // Llamada recursiva si este hijo también tiene sus propios hijos
            if (currChild.child != null) {
                printSubtree(currChild.child, indent + "    ");
            }

            currChild = currChild.next; // Avanzamos por el anillo de hermanos
        } while (currChild != childNode); // Terminamos al dar la vuelta completa
    }

}
