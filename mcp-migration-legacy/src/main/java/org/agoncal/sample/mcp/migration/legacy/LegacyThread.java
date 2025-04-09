package org.agoncal.sample.mcp.migration.legacy;

public class LegacyThread {

    public void useDeprecatedThreadMethods() {
        Thread thread = new Thread(() -> System.out.println("Running thread"));
        thread.suspend();
        thread.resume();
        thread.stop();
    }
}
