package com.ytdinfo.inndoo.common.rabbit;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.common.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by timmy on 2019/7/16.
 */
@Service
@Slf4j
public class RabbitUtil {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    public String getExchageName(String prefix, QueueEnum queueEnum) {
        if(StrUtil.isEmpty(prefix)){
            return queueEnum.getExchange();
        }
        return prefix + "." + queueEnum.getExchange();
    }

    public String getQueueName(String prefix, QueueEnum queueEnum) {
        if(StrUtil.isEmpty(prefix)){
            return queueEnum.getName();
        }
        return prefix + "." + queueEnum.getName();
    }

    /**
     * 向队列发送消息
     *
     * @param queueName
     * @param content
     */
    public void sendToQueue(String queueName, MQMessage content) {
        if(StrUtil.isEmpty(content.getAppid())) {
            content.setAppid(UserContext.getAppid());
        }
        if(StrUtil.isEmpty(content.getTenantId())) {
            content.setTenantId(UserContext.getTenantId());
        }
//        if(StrUtil.isEmpty(content.getTenantId())){
//            throw new InndooException("The tenantId is required, please set tenantId and try it again.");
//        }
        MessageProperties properties = new MessageProperties();
        properties.setContentEncoding("utf-8");
        properties.setContentType("text/plain");
        String jsonContent = JSONUtil.toJsonStr(content);
        Message message = new Message(StrUtil.bytes(jsonContent,CharsetUtil.CHARSET_UTF_8), properties);
        rabbitTemplate.convertAndSend(queueName, message);
    }

    /**
     * 向队列发送延迟消息
     *
     * @param queueName
     * @param content
     * @param delaySeconds
     */
    public void sendToQueueWithDelay(String queueName, MQMessage content, int delaySeconds) {
        if(StrUtil.isEmpty(content.getAppid())) {
            content.setAppid(UserContext.getAppid());
        }
        if(StrUtil.isEmpty(content.getTenantId())) {
            content.setTenantId(UserContext.getTenantId());
        }
//        if(StrUtil.isEmpty(content.getTenantId())){
//            throw new InndooException("The tenantId is required, please set tenantId and try it again.");
//        }
        MessageProperties properties = new MessageProperties();
        properties.setContentEncoding("utf-8");
        properties.setContentType("text/plain");
        //毫秒为单位
        properties.setExpiration(String.valueOf(delaySeconds * 1000));
        String jsonContent = JSONUtil.toJsonStr(content);
        Message message = new Message(StrUtil.bytes(jsonContent,CharsetUtil.CHARSET_UTF_8), properties);
        rabbitTemplate.convertAndSend(queueName, message);
    }

    /**
     * 向exchange发送消息
     *
     * @param exchange
     * @param content
     */
    public void sendToExchange(String exchange, MQMessage content) {
        sendToExchange(exchange, "", content);
    }

    /**
     * 向exchange发送消息
     *
     * @param exchange
     * @param routingKey
     * @param content
     */
    public void sendToExchange(String exchange, String routingKey, MQMessage content) {
        if(StrUtil.isEmpty(content.getAppid())) {
            content.setAppid(UserContext.getAppid());
        }
        if(StrUtil.isEmpty(content.getTenantId())) {
            content.setTenantId(UserContext.getTenantId());
        }
//        if(StrUtil.isEmpty(content.getTenantId())){
//            throw new InndooException("The tenantId is required, please set tenantId and try it again.");
//        }
        MessageProperties properties = new MessageProperties();
        properties.setContentEncoding(CharsetUtil.UTF_8);
        properties.setContentType("text/plain");
        String jsonContent = JSONUtil.toJsonStr(content);
        Message message = new Message(StrUtil.bytes(jsonContent,CharsetUtil.CHARSET_UTF_8), properties);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    /**
     * 向exchange发送消息
     *
     * @param exchange
     * @param routingKey
     * @param content
     * @param action
     */
    public void sendToExchange(String exchange, String routingKey, MQMessage content,String action) {
        if(StrUtil.isEmpty(content.getAppid())) {
            content.setAppid(UserContext.getAppid());
        }
        if(StrUtil.isEmpty(content.getTenantId())) {
            content.setTenantId(UserContext.getTenantId());
        }
//        if(StrUtil.isEmpty(content.getTenantId())){
//            throw new InndooException("The tenantId is required, please set tenantId and try it again.");
//        }
        MessageProperties properties = new MessageProperties();
        properties.setContentEncoding(CharsetUtil.UTF_8);
        properties.setContentType("text/plain");
        properties.getHeaders().put("action", action);
        String jsonContent = JSONUtil.toJsonStr(content);
        Message message = new Message(StrUtil.bytes(jsonContent,CharsetUtil.CHARSET_UTF_8), properties);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    /**
     * 转换Message为MQMessage对象
     *
     * @param message
     * @return
     */
    public MQMessage parseMessage(Message message) {
        String body = StrUtil.str(message.getBody(), CharsetUtil.CHARSET_UTF_8);
        JSONObject jsonObject = JSONUtil.parseObj(body);
        if(jsonObject.containsKey("message")){
            jsonObject.remove("message");
        }
        MQMessage mqMessage = JSONUtil.toBean(jsonObject,MQMessage.class);
        mqMessage.setMessage(message);
        return mqMessage;
    }

    /**
     * 手工ack
     *
     * @param message
     * @param channel
     */
    public void ack(Message message, Channel channel) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
    /**
     * 手工拒绝消息至死信队列
     *
     * @param message
     * @param channel
     */
    public void reject(Message message, Channel channel) {
        try {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 创建队列，含死信队列参数
     * @param prefix
     * @param queueEnum
     * @return
     */
    public Queue buildQueue(String prefix, QueueEnum queueEnum){
        String queueName = getQueueName(prefix, queueEnum);
        Map arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "dead-letter-exchange");
        arguments.put("x-dead-letter-routing-key", queueName);
        Queue queue = new Queue(queueName, true, false, false, arguments);
        return queue;
    }

    /**
     * 创建队列和交换机，含死信队列参数
     * @param exchangePrefix
     * @param queuePrefix
     * @param queueEnum
     * @return
     */
    public FanoutExchange createFanoutExchange(String exchangePrefix, String queuePrefix, QueueEnum queueEnum) {
        //创建交换机
        String exchageName = getExchageName(exchangePrefix,queueEnum);
        FanoutExchange exchange = (FanoutExchange) ExchangeBuilder.fanoutExchange(exchageName).durable(true).build();
        rabbitAdmin.declareExchange(exchange);
        if(StrUtil.isEmpty(queueEnum.getName())){
            return exchange;
        }
        String queueName = getQueueName(queuePrefix, queueEnum);
        String deathExchangeName = "dead-letter-exchange";
        DirectExchange deathExchange = (DirectExchange) ExchangeBuilder.directExchange(deathExchangeName).durable(true).build();
        rabbitAdmin.declareExchange(deathExchange);
        String deathQueueName = "z.dead." + queueName;
        Queue deadQueue = new Queue(deathQueueName, true, false, false);
        rabbitAdmin.declareQueue(deadQueue);
        Binding deadBinding = BindingBuilder.bind(deadQueue).to(deathExchange).with(queueName);
        rabbitAdmin.declareBinding(deadBinding);

        //创建队列
        Map arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "dead-letter-exchange");
        arguments.put("x-dead-letter-routing-key", queueName);
        Queue queue = new Queue(queueName, true, false, false, arguments);

        rabbitAdmin.declareQueue(queue);
        //绑定队列和交换机
        Binding binding = BindingBuilder.bind(queue).to(exchange);
        rabbitAdmin.declareBinding(binding);
        return exchange;
    }

    public Queue createQueue(String prefix, QueueEnum queueEnum){
        //创建队列
        String queueName = getQueueName(prefix, queueEnum);
        Map arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", "dead-letter-exchange");
        arguments.put("x-dead-letter-routing-key", queueName);
        Queue queue = new Queue(queueName, true, false, false, arguments);

        rabbitAdmin.declareQueue(queue);
        return queue;
    }

    /**
     * 绑定队列处理器
     * @param prefix
     * @param queueEnum
     * @param listener
     * @return
     */
    public Queue bindListener(String prefix,QueueEnum queueEnum, BaseConsumer listener){
        return bindListener(prefix,queueEnum,listener,false);
    }

    /**
     * 绑定队列处理器
     * @param prefix
     * @param queueEnum
     * @param listener
     * @param manualStart
     * @return
     */
    public Queue bindListener(String prefix,QueueEnum queueEnum, BaseConsumer listener, Boolean manualStart){
        Queue queue = buildQueue(prefix,queueEnum);
        ApplicationContext applicationContext = SpringContextUtil.getApplicationContext();
        //获取BeanFactory
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        //创建bean信息
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(SimpleMessageListenerContainer.class);
        beanDefinitionBuilder.addConstructorArgReference("connectionFactory");
        String queueName =  getQueueName(prefix,queueEnum);
        //动态注册bean
        String beanName = "simpleMessageListenerContainer" + queueName;
        //判断bean是否生成，如果生成删除重新生成
        if(defaultListableBeanFactory.containsBeanDefinition(beanName)){
            defaultListableBeanFactory.removeBeanDefinition(beanName);
        }
        defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        //获取动态注册的bean
        SimpleMessageListenerContainer container = SpringContextUtil.getBean(beanName, SimpleMessageListenerContainer.class);
        container.setQueues(queue);
        container.setExposeListenerChannel(true);
        //设置每个消费者获取的最大的消息数量
        container.setPrefetchCount(10);
        //消费者个数
        container.setConcurrentConsumers(2);
        container.setMaxConcurrentConsumers(20);
        //设置确认模式为手工确认
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        //监听处理类
        container.setMessageListener(listener);
        if(manualStart){
            container.start();
        }
        return queue;
    }
}