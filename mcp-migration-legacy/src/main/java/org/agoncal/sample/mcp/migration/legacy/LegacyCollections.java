package org.agoncal.sample.mcp.migration.legacy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyCollections {

    public void useLegacyCollections() {
        List<String> singletonList = Collections.singletonList("single");
        Map<String, String> singletonMap = Collections.singletonMap("key", "value");
        Map<String, String> unmodifiableMap = Collections.unmodifiableMap(new HashMap<>());

        System.out.println("Singleton List: " + singletonList);
        System.out.println("Singleton Map: " + singletonMap);
        System.out.println("Unmodifiable Map: " + unmodifiableMap);
    }

    public String useLegacyIterator() {
        Collection<String> collection = Collections.singletonList("single");
        String first = collection.iterator().next();
        return first;
    }

    public void useLegacyMap() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value");
        map.put("key2", "value");
        map.put("key3", "value");
    }
}
