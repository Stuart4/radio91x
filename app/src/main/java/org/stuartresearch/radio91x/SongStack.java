package org.stuartresearch.radio91x;

/**
 * Created by jake on 6/14/15.
 */
public class SongStack {
    private int[] data;
    private int first = -1;
    private int size = 0;

    public SongStack(int cap) {
        data = new int[cap];
    }

    public SongStack() {
        this(50);
    }

    //insert at end
    public void insert(int songInfo) {
        first++;
        first = first % data.length;
        size++;

        data[first] = songInfo;
    }

    //return counting from the back
    public int get(int pos) {
        if (pos > size() - 1) {
            throw new IndexOutOfBoundsException("position > size - 1");
        }

        int spot = first - pos;
        spot = (((spot % data.length) + data.length) % data.length); //real modulus


        return data[spot];
    }

    public int size() {
        return Math.min(size, data.length);
    }
}
