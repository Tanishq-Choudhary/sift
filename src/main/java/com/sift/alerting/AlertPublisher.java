package com.sift.alerting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sift.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class AlertPublisher implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertPublisher.class);
    private static final String QUEUE_NAME = "sift.alerts";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Connection connection;
    private final Channel channel;

    public AlertPublisher(String host, int port) throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        this.connection = connectionFactory.newConnection();
        this.channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    }

    public void publish(Alert alert) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(alert);
            channel.basicPublish("", QUEUE_NAME, null, body);
        } catch (Exception e) {
            LOGGER.warn("Failed to publish alert to RabbitMQ: {}", e.getMessage());
        }
    }

    @Override
    public void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}