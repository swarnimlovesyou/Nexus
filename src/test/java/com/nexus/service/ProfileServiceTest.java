package com.nexus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.nexus.dao.UserDao;
import com.nexus.domain.RegularUser;
import com.nexus.domain.User;

public class ProfileServiceTest {

    @Test
    public void getIntSettingShouldUseValueOrFallback() {
        ProfileService profileService = new ProfileService();
        int userId = ensureTestUser();
        String scope = ProfileService.GLOBAL_SCOPE;

        assertEquals(3, profileService.getIntSetting(userId, scope, "routing.max_fallback_candidates", 3));

        profileService.setSetting(userId, scope, "routing.max_fallback_candidates", "5");
        assertEquals(5, profileService.getIntSetting(userId, scope, "routing.max_fallback_candidates", 3));

        profileService.setSetting(userId, scope, "routing.max_fallback_candidates", "not-a-number");
        assertEquals(3, profileService.getIntSetting(userId, scope, "routing.max_fallback_candidates", 3));
    }

    @Test
    public void actionPoliciesShouldDefaultAllowAndRespectOverride() {
        ProfileService profileService = new ProfileService();
        int userId = ensureTestUser();
        String scope = "policy-test-" + System.nanoTime();

        assertTrue(profileService.isActionAllowed(userId, scope, "policy.allow_tool_shell"));

        profileService.setSetting(userId, scope, "policy.allow_tool_shell", "false");
        assertFalse(profileService.isActionAllowed(userId, scope, "policy.allow_tool_shell"));
    }

    private int ensureTestUser() {
        UserDao userDao = new UserDao();
        Optional<User> existing = userDao.findByUsername("profile_test_user");
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        RegularUser user = new RegularUser(null, "profile_test_user", "test-hash", LocalDateTime.now());
        userDao.create(user);
        return user.getId();
    }
}