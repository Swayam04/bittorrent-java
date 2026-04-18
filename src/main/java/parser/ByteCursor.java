package parser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

public class ByteCursor {
    private final byte[] data;
    private int position;
    private final int limit;
    private final Deque<Integer> marks;

    private ByteCursor(byte[] data, int position, int limit) {
        validateConstruction(data, position, limit);
        this.data = data;
        this.position = position;
        this.limit = limit;
        marks = new ArrayDeque<>();
    }

    public ByteCursor(byte[] data, int limit) {
        this(data, 0, limit);
    }

    public ByteCursor(byte[] data) {
        this(data, 0, data.length);
    }

    public byte readByte() {
        require(1);
        return data[position++];
    }

    public byte[] readBytes(int n) {
        require(n);
        byte[] nextBytes = copyBytes(n);
        position += n;
        return nextBytes;
    }

    public byte peekByte() {
        require(1);
        return data[position];
    }

    public byte[] peekBytes(int n) {
        require(n);
        return copyBytes(n);
    }

    public int peekUnsignedByte() {
        require(1);
        return data[position] & 0xFF;
    }

    public int readUnsignedByte() {
        require(1);
        return data[position++] & 0xff;
    }

    public short readShortBE() {
        return (short) readUnsignedShortBE();
    }

    public short readShortLE() {
        return (short) readUnsignedShortLE();
    }

    public int readUnsignedShortLE() {
        int b1 = readUnsignedByte();
        int b2 = readUnsignedByte();
        return (b2 << 8) | b1;
    }

    public int readUnsignedShortBE() {
        int b1 = readUnsignedByte();
        int b2 = readUnsignedByte();
        return (b1 << 8) | b2;
    }

    public long readUnsignedIntBE() {
        int b1 = readUnsignedByte();
        int b2 = readUnsignedByte();
        int b3 = readUnsignedByte();
        int b4 = readUnsignedByte();

        return ((long) b1 << 24) | ((long) b2 << 16) | ((long) b3 << 8) | b4;
    }

    public long readUnsignedIntLE() {
        int b1 = readUnsignedByte();
        int b2 = readUnsignedByte();
        int b3 = readUnsignedByte();
        int b4 = readUnsignedByte();

        return ((long) b4 << 24) | ((long) b3 << 16) | ((long) b2 << 8) | b1;
    }

    public int readIntBE() {
        return (int) readUnsignedIntBE();
    }

    public int readIntLE() {
        return (int) readUnsignedIntLE();
    }

    public long readLongBE() {
        long high = readUnsignedIntBE();
        long low = readUnsignedIntBE();
        return (high << 32) | low;
    }

    public long readLongLE() {
        long low = readUnsignedIntLE();
        long high = readUnsignedIntLE();
        return (high << 32) | low;
    }

    public void mark() {
        marks.push(position);
    }

    public void reset() {
        position = removeMark();
    }

    public void commit() {
        removeMark();
    }

    private int removeMark() {
        if (marks.isEmpty()) {
            throw new NoSuchElementException("No existing marks!");
        }
        return marks.pop();
    }

    public int position() {
        return position;
    }

    public int limit() {
        return limit;
    }

    public void position(int newPosition) {
        validatePosition(newPosition, limit);
        position = newPosition;
    }

    public void skip(int n) {
        require(n);
        position += n;
    }

    public int remaining() {
        return limit - position;
    }

    public boolean hasRemaining() {
        return position < limit;
    }

    public ByteCursor slice(int len) {
        require(len);
        ByteCursor viewCursor = new ByteCursor(data, position, position + len);
        position += len;
        return viewCursor;
    }

    public boolean startsWith(byte[] prefix) {
        if (remaining() < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[position + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    // TODO: Convert To KMP
    public byte[] readUntil(byte[] pattern) {
        if (pattern.length == 0) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }

        for (int i = position; i <= limit - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }

            if (match) {
                int length = i - position;
                byte[] result = copyBytes(length);
                position = i + pattern.length;
                return result;
            }
        }
        throw new NoSuchElementException("Pattern not found in remaining buffer!");
    }

    public String hexDump(int len) {
        int max = Math.min(len, remaining());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(String.format("%02X ", data[position + i] & 0xFF));
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return "ByteCursor[pos=" + position + ", remaining=" + remaining() + "]";
    }

    public String readUtf8(int len) {
        return new String(readBytes(len), StandardCharsets.UTF_8);
    }

    public char readChar() {
        return (char) readUnsignedByte();
    }

    public String readNullTerminatedString() {
        int startPos = position;
        while (position < limit && data[position] != 0) {
            position++;
        }
        if (position >= limit) {
            throw new IndexOutOfBoundsException("No null terminator found before reaching limit!");
        }
        int length = position - startPos;
        String result = new String(data, startPos, length, StandardCharsets.UTF_8);
        position++;
        return result;
    }

    private byte[] copyBytes(int len) {
        byte[] copiedBytes = new byte[len];
        System.arraycopy(data, position, copiedBytes, 0, len);
        return copiedBytes;
    }

    private void validateConstruction(byte[] data, int initialPosition, int limit) {
        validateLimit(data, limit);
        validatePosition(initialPosition, limit);
    }

    private void validatePosition(int pos, int limit) {
        if (pos < 0 || pos > limit) {
            throw new IndexOutOfBoundsException(
                    "pos=" + pos + ", limit=" + limit
            );
        }
    }
    private void validateLimit(byte[] data, int limit) {
        if (limit < 0 || limit > data.length) {
            throw new IllegalArgumentException("Limit invalid or greater than data length!");
        }
    }

    public void require(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be >= 0");
        }
        if (remaining() < n) {
            throw new IndexOutOfBoundsException(
                    "Need " + n + " bytes, remaining " + remaining()
            );
        }
    }
}
