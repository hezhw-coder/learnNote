package com.he_zhw.springamqpdemo.Listener;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class SpringRabbitListener {

//    @RabbitListener(queues = "simple.queue")
//    public void listenSimpleQueueMessage(String msg){
//        System.out.println("spring 消费者接收到消息 :["+ msg +"]");
//    }


//    @RabbitListener(queues = "simple.queue")
//    public void WorkQueueMessage1(String msg) throws InterruptedException {
//        System.out.println("WorkQueueMessage1 消费者接收到消息 :[" + msg + "]" + LocalDateTime.now());
//        Thread.sleep(20);
//    }
//
//
//    @RabbitListener(queues = "simple.queue")
//    public void WorkQueueMessage2(String msg) throws InterruptedException {
//        System.err.println("WorkQueueMessage2 消费者接收到消息 :[" + msg + "]" + LocalDateTime.now());
//        Thread.sleep(200);
//
//    }

    @RabbitListener(queues = "fanout.queue1")
    public void FanoutQueueMessage1(String msg) throws InterruptedException {
        System.out.println("FanoutQueue1 消费者接收到消息 :[" + msg + "]" + LocalDateTime.now());

    }

    @RabbitListener(queues = "fanout.queue2")
    public void FanoutQueueMessage2(String msg) throws InterruptedException {
        System.err.println("FanoutQueue2 消费者接收到消息 :[" + msg + "]" + LocalDateTime.now());

    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "direct.queue1"),exchange = @Exchange(name = "DirectDemo",type = ExchangeTypes.DIRECT),key  = {"red","blue"}))
    public void DirectQueueMessage1(String msg)  {
        System.out.println("DirectQueue1 消费者接收到消息 :[" + msg + "]" + LocalDateTime.now());

    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "direct.queue2"),exchange = @Exchange(name = "DirectDemo"),key = {"red","yellow"}))
    public void DirectQueueMessage2(String msg)  {
        System.err.println("DirectQueue2 消费者接收到消息 :[" + msg + "]" + LocalDateTime.now());

    }


    @RabbitListener(queues = "simple.queue")
    public void listenSimpleQueueMessage(Map<String, Object> msg) {
        System.out.println("spring 消费者接收到消息 :[" + msg + "]");
    }
}



