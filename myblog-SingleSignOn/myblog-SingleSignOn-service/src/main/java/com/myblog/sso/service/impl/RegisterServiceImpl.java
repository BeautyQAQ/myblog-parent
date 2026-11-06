package com.myblog.sso.service.impl;

import com.myblog.common.security.PasswordSecurity;
import com.myblog.common.utils.Result;
import com.myblog.mapper.TbUserMapper;
import com.myblog.pojo.TbUser;
import com.myblog.pojo.TbUserExample;
import com.myblog.sso.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class RegisterServiceImpl implements RegisterService {
    @Autowired
    private TbUserMapper userMapper;

    @Override
    public Result checkData(String param, int type) {
        if (param == null || param.isBlank() || param.length() > 50) {
            return Result.build(400, "请输入有效数据");
        }
        TbUserExample example = new TbUserExample();
        TbUserExample.Criteria criteria = example.createCriteria();
        if (type == 1) {
            criteria.andUsernameEqualTo(param.trim());
        } else if (type == 2) {
            criteria.andPhoneEqualTo(param.trim());
        } else if (type == 3) {
            criteria.andEmailEqualTo(param.trim());
        } else {
            return Result.build(400, "数据类型错误");
        }
        List<TbUser> users = userMapper.selectByExample(example);
        return Result.ok(users == null || users.isEmpty());
    }

    @Override
    public Result register(TbUser submitted) {
        if (submitted == null) {
            return Result.build(400, "用户数据不完整");
        }
        String username = nullableText(submitted.getUsername());
        String password = submitted.getPassword();
        String phone = nullableText(submitted.getPhone());
        String email = nullableText(submitted.getEmail());
        if (username == null || username.length() < 3 || username.length() > 50
                || password == null || password.isBlank() || password.length() < 8 || password.length() > 128
                || (phone != null && phone.length() > 20)
                || (email != null && (email.length() > 50 || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")))) {
            return Result.build(400, "用户名需为3至50字符，密码需为8至128字符，请检查电话和邮箱格式");
        }
        if (!Boolean.TRUE.equals(checkData(username, 1).getData())) {
            return Result.build(400, "此用户名已经被占用");
        }
        if (phone != null && !Boolean.TRUE.equals(checkData(phone, 2).getData())) {
            return Result.build(400, "手机号已经被占用");
        }
        if (email != null && !Boolean.TRUE.equals(checkData(email, 3).getData())) {
            return Result.build(400, "邮箱已经被占用");
        }
        // Construct a new record even for RPC callers: client-supplied id, role and timestamps are ignored.
        TbUser user = new TbUser();
        user.setUsername(username);
        user.setPassword(PasswordSecurity.hash(password));
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(TbUser.ROLE_USER);
        user.setCreated(new Date());
        user.setUpdated(new Date());
        try {
            return userMapper.insert(user) == 1 ? Result.ok() : Result.build(400, "注册失败，请稍后重试");
        } catch (DuplicateKeyException e) {
            return Result.build(400, "用户名、手机号或邮箱已经被占用");
        }
    }

    private static String nullableText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
