package com.recommend.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * 心跳推送服务 -- 生产环境 7x24 常驻
 * 每 20 秒向业务 Topic user_behavior 发送一条 heartbeat_user_001 行为消息
 * 保证 Flink 3 分钟窗口永远有该用户数据，缓存不失效
 */
public class HeartbeatProducer {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop102:9092,hadoop103:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        String topic = "user_behavior";

        String uid = "heartbeat_user_001";
        String itemId = "1535294";

        System.out.println("Heartbeat Producer started - sending to " + topic + " every 5s");
        System.out.println("User: " + uid + ", Item: " + itemId);

        long count = 0;
        while (true) {
            long now = System.currentTimeMillis() / 1000;
            String msg = uid + "," + itemId + "," + itemId + ",buy," + now;
            producer.send(new ProducerRecord<>(topic, null, msg));
            count++;
            if (count % 100 == 0) {
                System.out.println("Heartbeat sent " + count + " messages");
            }
            Thread.sleep(5_000);
        }
    }
}
