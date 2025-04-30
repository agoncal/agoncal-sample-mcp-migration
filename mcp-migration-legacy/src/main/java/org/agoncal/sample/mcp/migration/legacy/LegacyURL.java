package org.agoncal.sample.mcp.migration.legacy;

import java.net.URI;
import java.net.URL;

public class LegacyURL {

    public void useDeprecatedURLConstructor() throws Exception {
        URL url = URI.create("http://example.com").toURL();
        System.out.println("URL: " + url);
    }
}
