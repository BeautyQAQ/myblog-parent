package com.myblog.portal.message;

import com.myblog.common.jedis.JedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * 监听文章改动消息，接收消息后，将缓存清理，广播消息
 * <p>Title: ArticleAddMessageListener</p>
 * <p>Description: </p>
 * @version 1.0
 */
public class ArticleAddMessageListener implements MessageListener {

	@Autowired
	private JedisClient jedisClient;

	@Value("${CONTENT_LIST}")
	private String CONTENT_LIST;

	@Override
	public void onMessage(Message message) {
		try {
			//从消息中取商品id
			TextMessage textMessage = (TextMessage) message;
			String text = textMessage.getText();
			System.out.println("消息队列拿到的广播text：" + text);
			if ("delete_cache".equals(text)){
				//等待事务提交
				Thread.sleep(1000);
				//清空redis文章缓存
				jedisClient.del(CONTENT_LIST);
				System.out.println("缓存已经清空");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
