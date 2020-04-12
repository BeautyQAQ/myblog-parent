package com.myblog.sso.service;

import com.myblog.common.utils.Result;
import com.myblog.pojo.TbUser;

public interface RegisterService {
    Result checkData(String param, int type);
    Result register(TbUser user);
}
