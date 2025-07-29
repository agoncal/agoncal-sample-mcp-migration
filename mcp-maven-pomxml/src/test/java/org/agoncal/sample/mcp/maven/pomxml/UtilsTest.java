package org.agoncal.sample.mcp.maven.pomxml;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void testIsProfileNull_withNullValue_returnsTrue() {
        assertTrue(Utils.isProfileNull(null));
    }

    @Test
    void testIsProfileNull_withEmptyString_returnsTrue() {
        assertTrue(Utils.isProfileNull(""));
    }

    @Test
    void testIsProfileNull_withWhitespaceOnlyString_returnsTrue() {
        assertTrue(Utils.isProfileNull("   "));
        assertTrue(Utils.isProfileNull("\t"));
        assertTrue(Utils.isProfileNull("\n"));
        assertTrue(Utils.isProfileNull(" \t \n "));
    }

    @Test
    void testIsProfileNull_withNullStringLowercase_returnsTrue() {
        assertTrue(Utils.isProfileNull("null"));
    }

    @Test
    void testIsProfileNull_withNullStringUppercase_returnsTrue() {
        assertTrue(Utils.isProfileNull("NULL"));
    }

    @Test
    void testIsProfileNull_withNullStringMixedCase_returnsTrue() {
        assertTrue(Utils.isProfileNull("Null"));
        assertTrue(Utils.isProfileNull("nULL"));
        assertTrue(Utils.isProfileNull("NuLl"));
    }

    @Test
    void testIsProfileNull_withNullStringWithWhitespace_returnsTrue() {
        assertTrue(Utils.isProfileNull("  null  "));
        assertTrue(Utils.isProfileNull("\tnull\t"));
        assertTrue(Utils.isProfileNull(" NULL "));
        assertTrue(Utils.isProfileNull("  Null  "));
    }

    @Test
    void testIsProfileNull_withValidProfileId_returnsFalse() {
        assertFalse(Utils.isProfileNull("jakarta-ee"));
        assertFalse(Utils.isProfileNull("jacoco"));
        assertFalse(Utils.isProfileNull("production"));
        assertFalse(Utils.isProfileNull("test"));
        assertFalse(Utils.isProfileNull("development"));
    }

    @Test
    void testIsProfileNull_withStringContainingNull_returnsFalse() {
        assertFalse(Utils.isProfileNull("nullable"));
        assertFalse(Utils.isProfileNull("nullish"));
        assertFalse(Utils.isProfileNull("not-null"));
        assertFalse(Utils.isProfileNull("null-profile"));
        assertFalse(Utils.isProfileNull("profile-null"));
    }

    @Test
    void testIsProfileNull_withNumericStrings_returnsFalse() {
        assertFalse(Utils.isProfileNull("1"));
        assertFalse(Utils.isProfileNull("123"));
        assertFalse(Utils.isProfileNull("0"));
    }

    @Test
    void testIsProfileNull_withSpecialCharacters_returnsFalse() {
        assertFalse(Utils.isProfileNull("@"));
        assertFalse(Utils.isProfileNull("#"));
        assertFalse(Utils.isProfileNull("$"));
        assertFalse(Utils.isProfileNull("profile-1"));
        assertFalse(Utils.isProfileNull("profile_test"));
    }
}