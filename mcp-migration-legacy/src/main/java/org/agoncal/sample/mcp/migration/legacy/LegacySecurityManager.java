package org.agoncal.sample.mcp.migration.legacy;

import java.net.InetAddress;
import java.security.Permission;

public class LegacySecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        // Legacy implementation
    }

    @Override
    public void checkMulticast(InetAddress maddr, byte ttl) {
        // Deprecated method
    }
}
