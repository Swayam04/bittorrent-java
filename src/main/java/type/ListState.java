package type;

import java.util.ArrayList;
import java.util.List;

public class ListState implements StructureState {
    private final List<Object> list = new ArrayList<>();

    @Override
    public void addElement(Object value) {
        list.add(value);
    }

    @Override
    public Object getStructure() {
        return list;
    }
}
