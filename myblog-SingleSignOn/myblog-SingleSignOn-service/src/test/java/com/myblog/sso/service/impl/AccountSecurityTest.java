package com.myblog.sso.service.impl;

import com.myblog.common.jedis.JedisClient;
import com.myblog.common.security.PasswordSecurity;
import com.myblog.common.utils.JsonUtils;
import com.myblog.common.utils.Result;
import com.myblog.mapper.TbUserMapper;
import com.myblog.pojo.TbUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountSecurityTest {
    private final Map<String, String> sessions = new HashMap<>();
    private TbUser databaseUser;
    private TbUser inserted;
    private boolean duplicateInsert;
    private RegisterServiceImpl register;
    private LoginServiceImpl login;
    private TokenServiceImpl tokens;

    @BeforeEach
    void setUp() {
        TbUserMapper mapper = (TbUserMapper) Proxy.newProxyInstance(TbUserMapper.class.getClassLoader(),
                new Class<?>[]{TbUserMapper.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "selectByExample" -> databaseUser == null ? List.of() : List.of(copy(databaseUser));
                    case "selectByPrimaryKey" -> databaseUser == null ? null : copy(databaseUser);
                    case "insert" -> {
                        if (duplicateInsert) throw new DuplicateKeyException("duplicate");
                        inserted = (TbUser) args[0];
                        yield 1;
                    }
                    case "updateByPrimaryKeySelective" -> {
                        databaseUser.setPassword(((TbUser) args[0]).getPassword());
                        yield 1;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        JedisClient redis = (JedisClient) Proxy.newProxyInstance(JedisClient.class.getClassLoader(),
                new Class<?>[]{JedisClient.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "get" -> sessions.get((String) args[0]);
                    case "setex" -> { sessions.put((String) args[0], (String) args[2]); yield "OK"; }
                    case "expire" -> 1L;
                    case "del" -> sessions.remove((String) args[0]) == null ? 0L : 1L;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        register = new RegisterServiceImpl();
        login = new LoginServiceImpl();
        tokens = new TokenServiceImpl();
        for (Object service : List.of(register, login, tokens)) {
            ReflectionTestUtils.setField(service, "userMapper", mapper);
        }
        for (Object service : List.of(login, tokens)) {
            ReflectionTestUtils.setField(service, "jedisClient", redis);
            ReflectionTestUtils.setField(service, "SESSION_EXPIRE", 1800);
        }
    }

    @Test
    void registrationNeverAcceptsInjectedIdentityRoleOrDatesAndNormalizesOptionalFields() {
        TbUser untrusted = new TbUser();
        untrusted.setId(1L);
        untrusted.setRole(TbUser.ROLE_ADMIN);
        untrusted.setUsername("student");
        untrusted.setPassword(" test-password ");
        untrusted.setPhone(" ");
        untrusted.setEmail("");
        assertEquals(200, register.register(untrusted).getStatus());
        assertNull(inserted.getId());
        assertEquals(TbUser.ROLE_USER, inserted.getRole());
        assertNull(inserted.getPhone());
        assertNull(inserted.getEmail());
        assertNotNull(inserted.getCreated());
        assertTrue(PasswordSecurity.matches(" test-password ", inserted.getPassword()));
        assertFalse(PasswordSecurity.matches("test-password", inserted.getPassword()));
    }

    @Test
    void rejectsInvalidRegistrationAndHandlesAConcurrentUniqueConstraintViolation() {
        TbUser user = new TbUser();
        user.setUsername("student");
        user.setPassword("short");
        assertEquals(400, register.register(user).getStatus());
        assertNull(inserted);
        user.setPassword("test-password");
        user.setEmail("not-an-email");
        assertEquals(400, register.register(user).getStatus());
        user.setEmail("student@example.test");
        duplicateInsert = true;
        assertEquals(400, register.register(user).getStatus());
    }

    @Test
    void upgradesMd5OnlyAfterSuccessfulLoginAndCreatesAnExpiringIdentityOnlySession() {
        databaseUser = account("5f4dcc3b5aa765d61d8327deb882cf99", TbUser.ROLE_ADMIN);
        assertEquals(400, login.userLogin("student", "wrong-password").getStatus());
        assertTrue(sessions.isEmpty());
        assertTrue(PasswordSecurity.isLegacyHash(databaseUser.getPassword()));
        Result result = login.userLogin("student", "password");
        assertEquals(200, result.getStatus());
        assertFalse(PasswordSecurity.isLegacyHash(databaseUser.getPassword()));
        assertTrue(PasswordSecurity.matches("password", databaseUser.getPassword()));
        TbUser cached = JsonUtils.jsonToPojo(sessions.get("SESSION:" + result.getData()), TbUser.class);
        assertEquals(7L, cached.getId());
        assertNull(cached.getPassword());
        assertNull(cached.getRole());
        assertEquals(400, login.userLogin(null, null).getStatus());
    }

    @Test
    void sessionsUseTheLatestDatabaseRoleAndStopWorkingAfterLogoutOrAccountDeletion() {
        databaseUser = account("not-returned", TbUser.ROLE_ADMIN);
        sessions.put("SESSION:active", JsonUtils.objectToJson(databaseUser));
        TbUser admin = (TbUser) tokens.getUserByToken("active").getData();
        assertEquals(TbUser.ROLE_ADMIN, admin.getRole());
        assertNull(admin.getPassword());
        databaseUser.setRole(TbUser.ROLE_USER);
        assertEquals(TbUser.ROLE_USER, ((TbUser) tokens.getUserByToken("active").getData()).getRole());
        assertEquals(200, tokens.logout("active").getStatus());
        assertEquals(201, tokens.getUserByToken("active").getStatus());
        sessions.put("SESSION:deleted", JsonUtils.objectToJson(databaseUser));
        databaseUser = null;
        assertEquals(201, tokens.getUserByToken("deleted").getStatus());
        assertFalse(sessions.containsKey("SESSION:deleted"));
        assertEquals(200, tokens.logout(null).getStatus());
    }

    private static TbUser account(String password, String role) {
        TbUser user = new TbUser();
        user.setId(7L);
        user.setUsername("student");
        user.setPassword(password);
        user.setRole(role);
        return user;
    }

    private static TbUser copy(TbUser user) {
        TbUser copy = account(user.getPassword(), user.getRole());
        copy.setId(user.getId());
        return copy;
    }
}
