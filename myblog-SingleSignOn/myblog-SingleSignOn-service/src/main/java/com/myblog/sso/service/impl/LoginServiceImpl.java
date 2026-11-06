package com.myblog.sso.service.impl;

import com.myblog.common.jedis.JedisClient;
import com.myblog.common.security.PasswordSecurity;
import com.myblog.common.utils.JsonUtils;
import com.myblog.common.utils.Result;
import com.myblog.mapper.TbUserMapper;
import com.myblog.pojo.TbUser;
import com.myblog.pojo.TbUserExample;
import com.myblog.sso.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private TbUserMapper userMapper;
    @Autowired
    private JedisClient jedisClient;
    @Value("${SESSION_EXPIRE}")
    private Integer SESSION_EXPIRE;

    @Override
    public Result userLogin(String username, String password) {
        if (username == null || username.isBlank() || username.length() > 50
                || password == null || password.isBlank() || password.length() > 128) {
            return Result.build(400, "用户名或密码错误");
        }
        TbUserExample example = new TbUserExample();
        example.createCriteria().andUsernameEqualTo(username.trim());
        List<TbUser> users = userMapper.selectByExample(example);
        if (users == null || users.isEmpty() || !PasswordSecurity.matches(password, users.getFirst().getPassword())) {
            return Result.build(400, "用户名或密码错误");
        }
        TbUser user = users.getFirst();
        if (PasswordSecurity.isLegacyHash(user.getPassword())) {
            TbUser update = new TbUser();
            update.setId(user.getId());
            update.setPassword(PasswordSecurity.hash(password));
            update.setUpdated(new Date());
            userMapper.updateByPrimaryKeySelective(update);
        }
        String token = UUID.randomUUID().toString();
        // Persist only the identity. Token lookups reload current roles and public profile data.
        TbUser identity = new TbUser();
        identity.setId(user.getId());
        jedisClient.setex("SESSION:" + token, SESSION_EXPIRE, JsonUtils.objectToJson(identity));
        return Result.ok(token);
    }
}
