package com.tntexfinance.crm.integration.service;

import com.google.gson.Gson;
import com.tntexfinance.crm.integration.reborn.CurlMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class KafkaProducerService {
  private final KafkaProducer<String, String> producer;
  private static final String TOPIC = "curl-commands";
  private static final Gson gson = new Gson();

  public KafkaProducerService(String bootstrapServers) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    this.producer = new KafkaProducer<>(props);
  }

  public void sendCurlCommand(CurlMessage curlMessage) {
    String jsonMessage = gson.toJson(curlMessage);
    ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, jsonMessage);
    producer.send(record, (metadata, exception) -> {
      if (exception != null) {
        System.err.println("Error sending message: " + exception.getMessage());
      } else {
        System.out.println("Message sent successfully to topic " + metadata.topic() +
          " partition " + metadata.partition() +
          " offset " + metadata.offset());
      }
    });
  }

  public void close() {
    producer.close();
  }
}
