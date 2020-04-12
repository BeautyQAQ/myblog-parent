package com.myblog.sso.service.impl;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.myblog.common.utils.Result;
import com.myblog.mapper.TbUserMapper;
import com.myblog.pojo.TbUser;
import com.myblog.pojo.TbUserExample;
import com.myblog.sso.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;

@Service
public class RegisterServiceImpl implements RegisterService {

    @Autowired
    private TbUserMapper userMapper;

    @Override
    public Result checkData(String param, int type) {
        //根据不同的type生成不同的查询条件
        TbUserExample example = new TbUserExample();
        TbUserExample.Criteria criteria = example.createCriteria();
        //1：用户名 2：手机号 3：邮箱
        if (type == 1) {
            criteria.andUsernameEqualTo(param);
        } else if (type == 2) {
            criteria.andPhoneEqualTo(param);
        } else if (type == 3) {
            criteria.andEmailEqualTo(param);
        } else {
            return Result.build(400, "数据类型错误");
        }
        //执行查询
        List<TbUser> list = userMapper.selectByExample(example);
        //判断结果中是否包含数据
        if (list != null && list.size()>0) {
            //如果有数据返回false
            return Result.ok(false);
        }
        //如果没有数据返回true
        return Result.ok(true);
    }

    @Override
    public Result register(TbUser user) {
        //数据有效性校验
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())
                || StringUtils.isBlank(user.getPhone())) {
            return Result.build(400, "用户数据不完整，注册失败");
        }
        //1：用户名 2：手机号 3：邮箱
        Result result = checkData(user.getUsername(), 1);
        if (!(boolean) result.getData()) {
            return Result.build(400, "此用户名已经被占用");
        }
        result = checkData(user.getPhone(), 2);
        if (!(boolean)result.getData()) {
            return Result.build(400, "手机号已经被占用");
        }
        //补全pojo的属性
        user.setCreated(new Date());
        user.setUpdated(new Date());
        //对密码进行md5加密
        String md5Pass = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
        user.setPassword(md5Pass);
        //把用户数据插入到数据库中
        int count=userMapper.insert(user);
        if (count>0){
            //返回添加成功
            System.out.println("serviceimpl+++++++++++:添加成功");
            return Result.ok();
        }else {
            //返回添加失败
            System.out.println("serviceimpl+++++++++++:参数错误");
            return Result.build(400, "参数错误");
        }
    }
}
