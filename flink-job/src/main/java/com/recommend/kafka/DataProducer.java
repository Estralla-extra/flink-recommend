package com.recommend.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

public class DataProducer {
    public static void main(String[] args) throws Exception {
        // CSV 路径：优先从命令行参数读取，否则使用默认路径
        String csvFile;
        if (args.length > 0) {
            csvFile = args[0];
        } else {
            csvFile = "D:\\桌面\\fsdownload\\mini_behavior.csv";
            System.out.println("未指定路径，使用默认: " + csvFile);
        }
        System.out.println("数据文件: " + csvFile);

        // 1. Kafka 生产者配置
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop102:9092,hadoop103:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        // 2. 创建 Kafka 生产者
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        String topic = "user_behavior";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

//                显示指定回调参数类型，避免编译器类型推断问题
                producer.send(new ProducerRecord<>(topic, null, line),
                        (org.apache.kafka.clients.producer.RecordMetadata metadata, Exception e) -> {
                            if (e != null) {
                                e.printStackTrace();
                            }
                        });

                count++;
                if (count % 10000 == 0) {
                    System.out.println("已发送" + count + " 条");
                }
//                模拟实时发送，约每秒10条
                Thread.sleep(50);
            }
            System.out.println("发送完毕，共" + count + " 条");
        } finally {
            producer.close();
        }
    }
}
