package openrewrite;

import java.net.URI;
import java.net.URL;

public class ToBeUpdated {

    public static void main(String[] args) throws Exception {

        // This code has to be updated by the URLConstructorToURICreate recipe
        URL url = new URL("http://www.google.com");
        URI uri = new URI("http://www.google.com");

        ToBeUpdated me = new ToBeUpdated();
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
