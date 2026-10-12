package com.loginsight.dsa.common;

import java.util.NoSuchElementException;

/**
 * Hand-written LIFO stack used by the iterative DFS augmenting search of Ford-Fulkerson
 * (docs/02 §8.3) — the syllabus wants the explicit depth-first exploration to be visible rather than
 * delegated to recursion or {@code java.util.ArrayDeque}.
 *
 * <p>Amortised O(1) push/pop; the backing array doubles when full.</p>
 *
 * @param <T> element type
 */
public final class CustomStack<T> {

    private static final int DEFAULT_CAPACITY = 16;

    private Object[] elements;
    private int size;

    public CustomStack() {
        this(DEFAULT_CAPACITY);
    }

    public CustomStack(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity must be >= 1 but was " + initialCapacity);
        }
        this.elements = new Object[initialCapacity];
    }

    public void push(T value) {
        if (size == elements.length) {
            grow();
        }
        elements[size++] = value;
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        if (size == 0) {
            throw new NoSuchElementException("stack is empty");
        }
        T value = (T) elements[--size];
        elements[size] = null;
        return value;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (size == 0) {
            throw new NoSuchElementException("stack is empty");
        }
        return (T) elements[size - 1];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    private void grow() {
        Object[] expanded = new Object[elements.length * 2];
        System.arraycopy(elements, 0, expanded, 0, size);
        elements = expanded;
    }
}
