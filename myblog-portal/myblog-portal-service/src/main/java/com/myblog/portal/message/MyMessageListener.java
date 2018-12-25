package com.myblog.portal.message;

import com.myblog.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class MyMessageListener implements MessageListener {

	@Autowired
	private JedisClient jedisClient;

	@Value("${CONTENT_LIST}")
	private String CONTENT_LIST;

	@Override
	public void onMessage(Message message) {
		try {
			//取消息内容
			TextMessage textMessage = (TextMessage) message;
			String text = textMessage.getText();
			System.out.println("监听到的消息：" + text);
			if ("delete_cache".equals(text)){
				//等待事务提交
				Thread.sleep(1000);
				//清空redis文章缓存
				jedisClient.del(CONTENT_LIST);
				System.out.println("缓存已经清空");
			}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
