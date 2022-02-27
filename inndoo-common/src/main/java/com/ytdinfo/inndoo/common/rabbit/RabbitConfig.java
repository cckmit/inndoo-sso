package com.ytdinfo.inndoo.common.rabbit;

import com.ytdinfo.conf.core.annotation.XxlConf;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitConfig {

    @XxlConf("matrix.mq.rabbit.address")
    String address;
    @XxlConf("matrix.mq.rabbit.username")
    String username;
    @XxlConf("matrix.mq.rabbit.password")
    String password;
    @XxlConf("matrix.mq.rabbit.virtualhost")
    String mqRabbitVirtualHost;
    @XxlConf("matrix.mq.rabbit.exchange.name")
    String exchangeName;
    @XxlConf("matrix.mq.rabbit.size")
    int queueSize;

    @XxlConf("matrix.mq.concurrent.consumers")

    int concurrentConsumers;
    @XxlConf("matrix.mq.prefetch.count")
    int prefetchCount;

    private String secret = "rPMYoxxUmZVnGh3n";

    //创建mq连接
    @Bean(name = "connectionFactory")
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();

        connectionFactory.setUsername(AESUtil.decrypt(username,secret));
        connectionFactory.setPassword(AESUtil.decrypt(password,secret));
        connectionFactory.setVirtualHost(mqRabbitVirtualHost);
        connectionFactory.getRabbitConnectionFactory().setRequestedChannelMax(2000);
        connectionFactory.setPublisherConfirms(true);

        //该方法配置多个host，在当前连接host down掉的时候会自动去重连后面的host

        connectionFactory.setAddresses(AESUtil.decrypt(address,secret));
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory){
        return new RabbitAdmin(connectionFactory);
    }
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {

        });
        /**
         * 当mandatory标志位设置为true时
         * 如果exchange根据自身类型和消息routingKey无法找到一个合适的queue存储消息
         * 那么broker会调用basic.return方法将消息返还给生产者
         * 当mandatory设置为false时，出现上述情况broker会直接将消息丢弃
         */
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {

        });
        //使用单独的发送连接，避免生产者由于各种原因阻塞而导致消费者同样阻塞
        rabbitTemplate.setUsePublisherConnection(true);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());

        return rabbitTemplate;
    }

//    @Bean
//    public String[] mqMsgQueues() throws AmqpException, IOException {
//        String[] queueNames = new String[queueSize];
//        connectionFactory().createConnection().createChannel(false).exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT, true);
//        for (int i = 1; i <= queueSize; i++) {
//            String queueName = String.format("%s.queue%d", "test2", i);
//            ConnectionFactory connectionFactory = connectionFactory();
//            Channel channel = connectionFactory.createConnection().createChannel(false);
//            channel.queueDeclare(queueName, true, false, false, null);
//            channel.queueBind(queueName, exchangeName, queueName);
//            HandleService service = SpringContextUtil.getBean(HandleService.class);
//            ApplicationContext applicationContext = SpringContextUtil.getApplicationContext();
//            //获取BeanFactory
//            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
//            //创建bean信息
//            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(SimpleMessageListenerContainer.class);
//            beanDefinitionBuilder.addConstructorArgReference("connectionFactory");
//            //动态注册bean
//            defaultListableBeanFactory.registerBeanDefinition("simpleMessageListenerContainer" + i, beanDefinitionBuilder.getBeanDefinition());
//            //获取动态注册的bean
//            SimpleMessageListenerContainer container = SpringContextUtil.getBean("simpleMessageListenerContainer" + i, SimpleMessageListenerContainer.class);
//            container.setQueueNames(queueName);
//            container.setExposeListenerChannel(true);
//            //设置每个消费者获取的最大的消息数量
//            container.setPrefetchCount(prefetchCount);
//            //消费者个数
//            container.setConcurrentConsumers(2);
//            container.setMaxConcurrentConsumers(100);
//            //设置确认模式为手工确认
//            container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
//            //监听处理类
//            System.out.println(service.hashCode());
//            container.setMessageListener(service);
//            queueNames[i - 1] = queueName;
//        }
//        return queueNames;
//    }
}