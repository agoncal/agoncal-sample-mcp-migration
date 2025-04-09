package org.agoncal.sample.mcp.migration.legacy;

import java.net.URL;

public class LegacyURL {

    public void useDeprecatedURLConstructor() throws Exception {
        URL url = new URL("http://example.com");
        System.out.println("URL: " + url);
    }
}
