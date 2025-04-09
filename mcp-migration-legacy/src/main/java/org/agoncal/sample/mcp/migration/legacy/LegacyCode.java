package org.agoncal.sample.mcp.migration.legacy;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LegacyCode {

    public static void main(String[] args) throws Exception {

        // This code has to be updated by the URLConstructorToURICreate recipe
        URL url = URI.create("http://www.google.com").toURL();
        URI uri = new URI("http://www.google.com");

        String s1 = String.format("My String", 1);
        String s2 = String.format("My String", 2);

        // Should be updated by the UseStringIsEmpty recipe
        if (0 < s1.length()) {
            System.out.println("s1 is not empty");
        }

        // Should be updated by the MigrateLogRecordSetMillisToSetInstant recipe
        LogRecord logRecord = new LogRecord(Level.ALL, "Message to log");
        logRecord.setMillis(System.currentTimeMillis());

        // Should be updated by the MigrateURLDecoderDecode recipe
        String decodedURL = URLDecoder.decode("http://www.google.com");

        // Should be updated by the IteratorNext recipe
        Collection collection = new ConcurrentLinkedDeque();
        collection.iterator().next();

        // Should be updated by the ListFirstAndLast recipe
        List<String> list = new ArrayList<>();
        list.get(0);

        // Should be updated by the MigrateCollectionsSingletonList recipe
        List<String> list2 = Collections.singletonList("Hello");

        LegacyCode me = new LegacyCode();
        me.getResume();
    }

    private void getResume() {
        Thread someThread = new Thread(() -> {
            // Thread logic here
        });
        someThread.start();
        // This code has to be updated by the ThreadStopUnsupported recipe
        someThread.stop();
    }
}
