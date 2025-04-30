package org.agoncal.sample.mcp.migration.legacy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class LegacyCode1 {

    private static final Logger logger = Logger.global;

    public void useDeprecatedLogger() {
        logger.info("Using deprecated Logger.global field");
    }

    public void useDeprecatedCollectionsSingletonList() {
        List<String> list = Collections.singletonList("item");
        logger.info("List: " + list);
    }

    public void useDeprecatedArraysAsList() {
        Integer[] array = Arrays.asList(1, 2, 3).toArray(new Integer[0]);
        logger.info("Array: " + Arrays.toString(array));
    }
}
