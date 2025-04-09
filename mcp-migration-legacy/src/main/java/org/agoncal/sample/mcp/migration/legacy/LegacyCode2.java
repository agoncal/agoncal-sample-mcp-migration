package org.agoncal.sample.mcp.migration.legacy;

import java.net.URL;
import java.security.KeyStore;
import java.util.Locale;

public class LegacyCode2 {

    public void useDeprecatedKeyStore() throws Exception {
        String defaultType = KeyStore.getDefaultType();
        System.out.println("Default KeyStore type: " + defaultType);
    }

    public void useDeprecatedLocaleConstructor() {
        Locale locale = new Locale("en", "US");
        System.out.println("Locale: " + locale);
    }

    public void useDeprecatedURLConstructor() throws Exception {
        URL url = new URL("http://example.com");
        System.out.println("URL: " + url);
    }
}
