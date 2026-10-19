package utec.hollowHeaps;

import utec.hollowHeaps.fibonacciHeap.FibonacciHeap;
import utec.hollowHeaps.fibonacciHeap.FibonacciHeapNode;
import utec.hollowHeaps.hollowHeap.HollowHeap;
import utec.hollowHeaps.hollowHeap.HollowItem;

public class Main {
    public static void main(String[] args) {
        FibonacciHeap heap = new FibonacciHeap();
        FibonacciHeapNode example =heap.enqueue(1,10, "Chanchito 1");
        heap.enqueue(2,20, "Chanchito 2");
        heap.enqueue(3,30, "Chanchito 3");
        heap.enqueue(4,40, "Chanchito 4");
        heap.enqueue(5,50, "Chanchito 5");
        heap.printHeapBySlots();
        heap.extractMin();
        heap.printHeapBySlots();
        heap.decreaseKey(example,1 );
        heap.printHeapBySlots();
    }
}