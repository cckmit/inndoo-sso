package com.ytdinfo.inndoo.common.rabbit;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Created by timmy on 2019/7/11.
 */
@Service
@Scope("prototype")
public class HandleService implements ChannelAwareMessageListener {
    @Override
    public void onMessage(Message message, Channel channel) {
//        ThreadPoolUtil.getPool().execute(() -> {
//            DynamicDataSourceContextHolder.setDataSourceType("appid");
//            String body = "";
//            try {
//                body = new String(message.getBody(),"utf-8");
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//            Gson gson = new Gson();
//            String content = gson.fromJson(body, String.class);
//            List<User> userList = userService.findAll();
//            System.out.println(userList.size());
//            System.out.println(content);
//            try {
//                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
    }
}