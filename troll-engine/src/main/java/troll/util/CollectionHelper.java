package troll.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CollectionHelper {

    public static <T> Set<T> createSet(T... elements) {
        var result = new HashSet<T>();
        Collections.addAll(result, elements);
        return result;
    }
}
