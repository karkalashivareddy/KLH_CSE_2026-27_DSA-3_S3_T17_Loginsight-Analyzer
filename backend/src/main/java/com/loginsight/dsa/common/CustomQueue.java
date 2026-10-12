package com.loginsight.dsa.common;

import java.util.NoSuchElementException;

/**
 * Hand-written FIFO queue used where the syllabus expects the queue itself to be visible
 * (docs/02 §8.3): the BFS traversals of Edmonds-Karp and Dinic, and the augmenting-search in the
 * matching reduction. A {@code java.util.ArrayDeque} would hide a trivial but real data structure
 * behind a library call, so the growable circular buffer is written out here.
 *
 * <p>This is deliberately small: {@code enqueue}/{@code dequeue}/{@code peek}/{@code isEmpty}. It is
 * generic rather than primitive-specialised because the flow algorithms queue vertex indices and the
 * clarity of a single transparent structure matters more than avoiding a few boxed integers.</p>
 *
 * <p>Amortised O(1) enqueue/dequeue; the backing array doubles when full.</p>
 *
 * @param <T> element type
 */
public final class CustomQueue<T> {

    private static final int DEFAULT_CAPACITY = 16;

    private Object[] elements;
    private int head;
    private int tail;
    private int size;

    public CustomQueue() {
        this(DEFAULT_CAPACITY);
    }

    public CustomQueue(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity must be >= 1 but was " + initialCapacity);
        }
        this.elements = new Object[initialCapacity];
    }

    public void enqueue(T value) {
        if (size == elements.length) {
            grow();
        }
        elements[tail] = value;
        tail = (tail + 1) % elements.length;
        size++;
    }

    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (size == 0) {
            throw new NoSuchElementException("queue is empty");
        }
        T value = (T) elements[head];
        elements[head] = null;
        head = (head + 1) % elements.length;
        size--;
        return value;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (size == 0) {
            throw new NoSuchElementException("queue is empty");
        }
        return (T) elements[head];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void clear() {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = null;
        }
        head = 0;
        tail = 0;
        size = 0;
    }

    private void grow() {
        Object[] expanded = new Object[elements.length * 2];
        for (int i = 0; i < size; i++) {
            expanded[i] = elements[(head + i) % elements.length];
        }
        elements = expanded;
        head = 0;
        tail = size;
    }
}
