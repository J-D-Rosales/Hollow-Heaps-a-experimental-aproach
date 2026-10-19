package utec.hollowHeaps.hollowHeap;

import java.util.ArrayList;
import java.util.List;

public class HollowHeap {

    HollowNode root;

    public HollowHeap(){
        buildHeap();
    }

    private void buildHeap(){
        this.root = null;
    }

    private HollowNode link(HollowNode u, HollowNode v){

        if (u == null) return v;
        if (v == null) return u;

        // Ambos nodos son roots
        if (u.key > v.key){
            HollowNode aux = v;
            v = u;
            u = aux;
        }
        v.next = u.child;
        u.child = v;

        return u;
    }

    public HollowItem findMin(){
        return this.root == null ? null : this.root.item;
    }

    public HollowItem insert(int key, String item){
        HollowNode nuevoNodo = new HollowNode(key, item);

        if (this.root == null){
            this.root = nuevoNodo;
        }
        else{
            this.root = link(this.root, nuevoNodo);
        }

        return nuevoNodo.item;
    }

    public void decreaseKey(HollowItem item, int newKey){
        HollowNode u = item.node;

        if (newKey >= u.key) {
            return;
        }

        // Caso de si u es root
        if (u == this.root) {
            u.key = newKey;
            return;
        }

        // Caso general
        // Creamos un nuevo nodo v con el item y la clave
        HollowNode v = new HollowNode(newKey,item);
        v.rank = Math.max(0,u.rank - 2);
        item.node = v;

        // El nodo anterior u pasa a ser Hollow
        u.item = null;

        // Asignamos padres
        v.child = u; // su unico hijo
        u.ep = v; // si no es el root asignamos un segundo padre

        this.root = link(this.root, v);
    }

    public HollowItem deleteMin(){

        if (this.root == null){
            return null;
        }

        // volvemos hollow a la raiz
        HollowItem minimoItem = this.root.item;
        this.root.item = null;

        // Reestructuracion del heap y eliminación de los Hollow
        List<HollowNode> L = new ArrayList<>();
        L.add(this.root);

        HollowNode[] A = new HollowNode[64]; // 64 rangos es suficiente para 2^64 elementos
        int maxRank = 0;


        while (!L.isEmpty()){
            HollowNode w = L.removeLast();
            HollowNode c = w.child;
            w.child = null; // W queda solo

            while (c != null){
                HollowNode nextC = c.next;

                if (c.item == null){
                    if (c.ep != null){
                        if (c.ep == w) {
                            nextC = null; // Si w era su segundo padre, era el último hijo
                        } else {
                            c.next = null;
                        }
                        c.ep = null;
                    }
                    else{
                        L.add(c);
                    }
                }
                else{ // C está activo por lo que hay que colocarlo en A y hacer coalesce
                    c.next = null;

                    while (A[c.rank] != null){
                        HollowNode otro = A[c.rank];
                        A[c.rank] = null;
                        c = link(c,otro);
                        c.rank = c.rank +1;
                    }
                    A[c.rank] = c;
                    maxRank = Math.max(maxRank,c.rank);
                }
                c = nextC;
            }
        }
        // Reconstruir la raiz global uniendo los árboles en A
        this.root = null;
        for (int i = 0; i <= maxRank; i++){
            if (A[i] != null){
                if (this.root == null){
                    this.root = A[i];
                }
                else{
                    this.root = link(this.root,A[i]);
                }
            }
        }
        return minimoItem;
    }
    public void printHeap() {
        // Definición de colores ANSI
        String RESET = "\u001B[0m";
        String AMARILLO = "\u001B[33m";

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

        // Códigos ANSI para los colores de los nodos
        String RESET = "\u001B[0m";
        String ROJO = "\u001B[38;5;208m";
        String VERDE = "\u001B[32m";
        String AZUL = "\u001B[34m";
        String GRIS = "\u001B[90m";

        // Prefijo gráfico para la consola
        System.out.print(GRIS + indent + (isLast ? "└── " : "├── ") + RESET);
        if (isLast) {
            indent += "    ";
        } else {
            indent += "│   ";
        }

        // Determinar el color según el tipo de nodo
        String colorNodo;
        String estado;

        if (node.item == null) {
            colorNodo = ROJO; // HOLLOW siempre en rojo
            estado = "HOLLOW";
        } else if (node.child == null) {
            colorNodo = VERDE; // Hojas (FULL sin hijos) en verde
            estado = "HOJA [Item: " + node.item + "]";
        } else {
            colorNodo = AZUL; // Nodos internos FULL en azul
            estado = "FULL [Item: " + node.item + "]";
        }

        String extraParent = (node.ep != null) ? " (EP -> Key: " + node.ep.key + ")" : "";

        // Imprimir detalles del nodo con su color correspondiente
        System.out.println(colorNodo + estado + " (Key: " + node.key + ", Rank: " + node.rank + ")" + extraParent + RESET);

        // Contar cuántos hijos tiene este nodo
        int childCount = 0;
        HollowNode curr = node.child;
        while (curr != null) {
            childCount++;
            curr = curr.next;
        }

        // Recorrer la lista de hijos
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
