package org.agoncal.sample.mcp.migration.legacy;

public class LegacyThread {

    public void useDeprecatedThreadMethods() {
        Thread thread = new Thread(() -> System.out.println("Running thread"));
        /*
         * `Thread.suspend()` always throws a `new UnsupportedOperationException()` in Java 21+.
         * For detailed migration instructions see the migration guide available at
         * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html
         */
        thread.suspend();
        /*
         * `Thread.resume()` always throws a `new UnsupportedOperationException()` in Java 21+.
         * For detailed migration instructions see the migration guide available at
         * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html
         */
        thread.resume();
        /*
         * `Thread.stop()` always throws a `new UnsupportedOperationException()` in Java 21+.
         * For detailed migration instructions see the migration guide available at
         * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html
         */
        thread.stop();
    }
}
