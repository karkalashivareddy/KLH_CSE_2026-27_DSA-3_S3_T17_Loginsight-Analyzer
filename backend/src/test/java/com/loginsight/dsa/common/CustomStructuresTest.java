package com.loginsight.dsa.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/**
 * Tests for the two hand-written structures the flow engine relies on: FIFO order, LIFO order,
 * wrap-around growth, clearing and the documented empty/invalid-argument behaviour.
 */
class CustomStructuresTest {

    @Test
    void queueIsFirstInFirstOut() {
        CustomQueue<Integer> queue = new CustomQueue<>();
        assertTrue(queue.isEmpty());
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);
        assertEquals(3, queue.size());
        assertEquals(1, queue.peek());
        assertEquals(1, queue.dequeue());
        assertEquals(2, queue.dequeue());
        assertEquals(3, queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void queueGrowsAndKeepsOrderAcrossWrapAround() {
        CustomQueue<Integer> queue = new CustomQueue<>(2);
        for (int i = 0; i < 10; i++) {
            queue.enqueue(i);
        }
        for (int i = 0; i < 5; i++) {
            assertEquals(i, queue.dequeue());
        }
        for (int i = 10; i < 15; i++) {
            queue.enqueue(i);
        }
        for (int i = 5; i < 15; i++) {
            assertEquals(i, queue.dequeue(), "order preserved after wrap-around");
        }
        assertTrue(queue.isEmpty());
    }

    @Test
    void queueClearResetsState() {
        CustomQueue<Integer> queue = new CustomQueue<>();
        queue.enqueue(1);
        queue.enqueue(2);
        queue.clear();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void queueRejectsInvalidOperations() {
        assertThrows(IllegalArgumentException.class, () -> new CustomQueue<Integer>(0));
        CustomQueue<Integer> queue = new CustomQueue<>();
        assertThrows(NoSuchElementException.class, queue::dequeue);
        assertThrows(NoSuchElementException.class, queue::peek);
    }

    @Test
    void stackIsLastInFirstOut() {
        CustomStack<Integer> stack = new CustomStack<>();
        assertTrue(stack.isEmpty());
        stack.push(1);
        stack.push(2);
        stack.push(3);
        assertEquals(3, stack.size());
        assertEquals(3, stack.peek());
        assertEquals(3, stack.pop());
        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    void stackGrows() {
        CustomStack<Integer> stack = new CustomStack<>(2);
        for (int i = 0; i < 20; i++) {
            stack.push(i);
        }
        for (int i = 19; i >= 0; i--) {
            assertEquals(i, stack.pop());
        }
    }

    @Test
    void stackRejectsInvalidOperations() {
        assertThrows(IllegalArgumentException.class, () -> new CustomStack<Integer>(-1));
        CustomStack<Integer> stack = new CustomStack<>();
        assertThrows(NoSuchElementException.class, stack::pop);
        assertThrows(NoSuchElementException.class, stack::peek);
        assertTrue(stack.isEmpty());
    }
}
