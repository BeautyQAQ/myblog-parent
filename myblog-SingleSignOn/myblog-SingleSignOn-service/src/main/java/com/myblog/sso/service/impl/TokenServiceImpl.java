package com.myblog.sso.service.impl;

import com.myblog.common.jedis.JedisClient;
import com.myblog.common.utils.JsonUtils;
import com.myblog.common.utils.Result;
import com.myblog.mapper.TbUserMapper;
import com.myblog.pojo.TbUser;
import com.myblog.sso.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenServiceImpl implements TokenService {
    @Autowired
    private JedisClient jedisClient;
    @Autowired
    private TbUserMapper userMapper;
    @Value("${SESSION_EXPIRE}")
    private Integer SESSION_EXPIRE;

    @Override
    public Result getUserByToken(String token) {
        if (token == null || token.isBlank() || token.length() > 200) {
            return expired();
        }
        String json = jedisClient.get("SESSION:" + token);
        if (json == null || json.isBlank()) {
            return expired();
        }
        TbUser identity;
        try {
            identity = JsonUtils.jsonToPojo(json, TbUser.class);
        } catch (RuntimeException e) {
            jedisClient.del("SESSION:" + token);
            return expired();
        }
        TbUser user = identity == null || identity.getId() == null ? null : userMapper.selectByPrimaryKey(identity.getId());
        if (user == null || (!TbUser.ROLE_USER.equals(user.getRole()) && !TbUser.ROLE_ADMIN.equals(user.getRole()))) {
            jedisClient.del("SESSION:" + token);
            return expired();
        }
        user.setPassword(null);
        jedisClient.expire("SESSION:" + token, SESSION_EXPIRE);
        return Result.ok(user);
    }

    @Override
    public Result logout(String token) {
        if (token != null && !token.isBlank() && token.length() <= 200) {
            jedisClient.del("SESSION:" + token);
        }
        return Result.ok();
    }

    private static Result expired() {
        return Result.build(201, "用户登录已经过期");
    }
}
