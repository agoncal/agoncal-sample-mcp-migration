package org.agoncal.sample.mcp.migration.legacy;

import java.sql.DriverManager;

public class LegacyDriverManager {

    public void useDeprecatedSetLogStream() {
        DriverManager.setLogStream(System.out);
    }
}
