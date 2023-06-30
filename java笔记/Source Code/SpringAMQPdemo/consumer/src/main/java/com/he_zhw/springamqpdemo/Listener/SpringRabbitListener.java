package com.he_zhw.springamqpdemo.Listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SpringRabbitListener {
    @RabbitListener(queues = "simple.queue")
    public void listenSimpleQueueMessage(String msg){
        System.out.println("spring 消费者接收到消息 :["+ msg +"]");
    }
}
