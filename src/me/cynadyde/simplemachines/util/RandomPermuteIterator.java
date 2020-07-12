package me.cynadyde.simplemachines.util;

import java.util.Iterator;
import java.util.Random;

/**
 * An iterator over an integer range that returns each
 * element in a random order once.
 */
public class RandomPermuteIterator implements Iterator<Integer> {

    // solution from: https://stackoverflow.com/a/29158917
    // also see: https://en.wikipedia.org/wiki/Linear_congruential_generator

    private static final int INCREMENT = 1013904223;
    private static final int MULTIPLIER = 1664525;

    private final int OFFSET;
    private final int SIZE;
    private final int MODULUS;
    private int seed;
    private int next;
    private boolean ended;

    public RandomPermuteIterator(int start, int end) {
        this(start, end, new Random());
    }

    public RandomPermuteIterator(int start, int end, Random seeder) {
        this.OFFSET = start;
        this.SIZE = end - start;
        this.MODULUS = (int) Math.pow(2, Math.ceil(Math.log(SIZE) / Math.log(2))); // round size to next power of 2
        this.seed = seeder.nextInt(SIZE);
        this.next = seed;
        this.ended = false;
    }

    @Override
    public boolean hasNext() {
        return !ended;
    }

    @Override
    public Integer next() {
        do { next = (MULTIPLIER * next + INCREMENT) % MODULUS; }
        while (next >= SIZE);
        if (next == seed) {
            ended = true;
        }
        return OFFSET + next;
    }
}
