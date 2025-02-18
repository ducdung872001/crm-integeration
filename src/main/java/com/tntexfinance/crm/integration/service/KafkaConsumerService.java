package com.tntexfinance.crm.integration.service;

import com.google.gson.Gson;
import com.tntexfinance.crm.integration.reborn.CurlMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Service
@Slf4j
public class KafkaConsumerService implements Runnable {
  private final KafkaConsumer<String, String> consumer;
  private static final String TOPIC = "curl-commands";
  private volatile boolean running = true;
  private static final Gson gson = new Gson();

  public KafkaConsumerService(String bootstrapServers, String groupId) {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    this.consumer = new KafkaConsumer<>(props);
    this.consumer.subscribe(Collections.singletonList(TOPIC));
  }

  @Override
  public void run() {
    try {
      while (running) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          CurlMessage curlMessage = gson.fromJson(record.value(), CurlMessage.class);
          executeCurl(curlMessage);
        }
      }
    } finally {
      consumer.close();
    }
  }

  private void executeCurl(CurlMessage curlMessage) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder();
      processBuilder.command("bash", "-c", curlMessage.getCurlCommand());
      Process process = processBuilder.start();

      int exitCode = process.waitFor();
      if (exitCode == 0) {
        System.out.println("Curl command executed successfully");
      } else {
        System.err.println("Curl command failed with exit code: " + exitCode);
      }
    } catch (Exception e) {
      System.err.println("Error executing curl command: " + e.getMessage());
    }
  }

  public void shutdown() {
    running = false;
  }
}
