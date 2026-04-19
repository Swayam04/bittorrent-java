package type;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BencodeString implements Comparable<BencodeString> {

    private final byte[] data;
    private String cachedString;

    public BencodeString(byte[] data) {
        this.data = data;
    }

    @Override
    public int compareTo(BencodeString o) {
        return compare(this.data, o.getData());
    }

    public static int compare(byte[] a, byte[] b) {
        return Arrays.compareUnsigned(a, b);
    }

    public byte[] getData() {
        return data;
    }


    @Override
    public String toString() {
        if (cachedString != null) {
            return cachedString;
        }
        CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            cachedString = charsetDecoder.decode(ByteBuffer.wrap(data)).toString();
            return cachedString;
        } catch (CharacterCodingException e) {
            throw new RuntimeException("Bencoded String cannot be converted to UTF_8 String", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (this == o) return true;
        return Arrays.equals(data, ((BencodeString) o).getData());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    public static BencodeString of(String key) {
        return new BencodeString(key.getBytes(StandardCharsets.UTF_8));
    }
}
