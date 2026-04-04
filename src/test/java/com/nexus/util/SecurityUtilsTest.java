package com.nexus.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecurityUtilsTest {

    @Test
    public void testHashAndVerify() {
        String password = "testPassword";
        String hashed = SecurityUtils.hashPassword(password);
        
        assertNotNull(hashed);
        assertNotEquals(password, hashed);
        assertTrue(SecurityUtils.verifyPassword(password, hashed));
        assertFalse(SecurityUtils.verifyPassword("wrongPassword", hashed));
    }
}
