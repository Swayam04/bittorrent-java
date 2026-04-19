package type;

import java.util.Map;
import java.util.TreeMap;

public class DictState implements StructureState {
    private final Map<BencodeString, Object> dict = new TreeMap<>();
    private BencodeString pendingKey = null;

    @Override
    public void addElement(Object value) {
        if (pendingKey == null) {
            if (!(value instanceof BencodeString)) {
                throw new RuntimeException("Invalid Key: Dict Key must be of type string");
            }
            pendingKey = (BencodeString) value;
        } else {
            dict.put(pendingKey, value);
            pendingKey = null;
        }
    }

    @Override
    public Object getStructure() {
        if (pendingKey != null) {
            throw new RuntimeException("Invalid Bencode: Dictionary missing value for key");
        }
        return dict;
    }
}
