package com.sift.detection;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.sift.alerting.AlertPublisher;
import com.sift.model.Alert;
import com.sift.model.AlertType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class AnomalyDetectionServiceIntegrationTest {

    private static final String QUEUE_NAME = "sift.alerts";

    private AlertPublisher alertPublisher;
    private AlertRingBuffer alertRingBuffer;

    @BeforeEach
    void setUp() throws Exception {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queuePurge(QUEUE_NAME);
        } catch (Exception ignored) {
        }

        alertPublisher = new AlertPublisher("localhost", 5672);
        alertRingBuffer = new AlertRingBuffer(500);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (alertPublisher != null) {
            alertPublisher.close();
        }
    }

    @Test
    void triggeredAlertReachesBothRingBufferAndRabbitMqQueue() throws Exception {
        Alert alert = new Alert(AlertType.PORT_SCAN, "198.51.100.5", "test alert", 25.0, Instant.now());

        alertRingBuffer.add(alert);
        alertPublisher.publish(alert);

        assertEquals(1, alertRingBuffer.size());

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            GetResponse response = channel.basicGet(QUEUE_NAME, true);
            assertNotNull(response, "expected a message on " + QUEUE_NAME);
            String body = new String(response.getBody());
            assertTrue(body.contains("PORT_SCAN"));
        }
    }
}