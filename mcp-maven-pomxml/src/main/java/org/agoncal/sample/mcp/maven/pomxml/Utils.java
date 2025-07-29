package org.agoncal.sample.mcp.maven.pomxml;

public class Utils {

    /**
     * Checks if a profileId represents a null or empty profile.
     * A profile is considered null if it is:
     * - null
     * - equals the string "null" (case insensitive)
     * - empty string
     * - contains only whitespace characters
     *
     * @param profileId the profile ID to check
     * @return true if the profile should be treated as null/main POM, false otherwise
     */
    public static boolean isProfileNull(String profileId) {
        return profileId == null ||
               "null".equalsIgnoreCase(profileId.trim()) ||
               profileId.trim().isEmpty();
    }
}