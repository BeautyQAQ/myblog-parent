package com.myblog.sso.service.impl;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.myblog.common.jedis.JedisClient;
import com.myblog.common.utils.JsonUtils;
import com.myblog.common.utils.Result;
import com.myblog.pojo.TbUser;
import com.myblog.sso.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenServiceImpl implements TokenService {

    @Autowired
    private JedisClient jedisClient;

    @Value("${SESSION_EXPIRE}")
    private Integer SESSION_EXPIRE;

    @Override
    public Result getUserByToken(String token) {
        //根据token到redis中取用户信息
        String json = jedisClient.get("SESSION:" + token);
        //取不到用户信息，登录已经过期，返回登录过期
        if (StringUtils.isBlank(json)) {
            return Result.build(201, "用户登录已经过期");
        }
        //取到用户信息更新token的过期时间
        jedisClient.expire("SESSION:" + token, SESSION_EXPIRE);
        //返回结果，E3Result其中包含TbUser对象
        TbUser user = JsonUtils.jsonToPojo(json, TbUser.class);
        return Result.ok(user);
    }
}
