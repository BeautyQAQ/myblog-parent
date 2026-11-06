package com.myblog.sso.controller;

import com.myblog.pojo.TbUser;
import org.apache.dubbo.common.utils.DefaultSerializeClassChecker;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityConfigurator;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

class SessionSerializationTest {
    @Test
    void accountDtoIsAllowedByThePackagedResourceWhileStrictStillRejectsOtherClasses() throws Exception {
        FrameworkModel framework = new FrameworkModel();
        try {
            var module = framework.newApplication().newModule();
            new SerializeSecurityConfigurator(module);
            var manager = framework.getBeanFactory().getBean(SerializeSecurityManager.class);
            manager.setCheckStatus(SerializeCheckStatus.STRICT);
            var checker = new DefaultSerializeClassChecker(framework);
            ClassLoader loader = getClass().getClassLoader();
            assertEquals(TbUser.class, checker.loadClass(loader, TbUser.class.getName()));
            assertThrows(IllegalArgumentException.class,
                    () -> checker.loadClass(loader, UnlistedPayload.class.getName()));
        } finally {
            framework.destroy();
        }
    }

    private static class UnlistedPayload implements Serializable {}
}
