package com.sift.alerting;

import com.sift.detection.AlertJsonLineLogger;
import com.sift.detection.AlertRingBuffer;
import com.sift.detection.AnomalyDetectionService;
import com.sift.detection.DnsTunnelingDetector;
import com.sift.detection.PortScanDetector;
import com.sift.detection.SynFloodDetector;
import com.sift.flow.FlowTable;
import com.sift.flow.FlowTrackingService;
import com.sift.model.Alert;
import com.sift.model.AlertType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AsyncAlertDispatcherTest {

    @Test
    void slowPublisherDoesNotBlockTheCaller() {
        AlertPublisher slowPublisher = mock(AlertPublisher.class);
        doAnswer(invocation -> {
            Thread.sleep(300);
            return null;
        }).when(slowPublisher).publish(any());
        AlertJsonLineLogger logger = mock(AlertJsonLineLogger.class);

        AnomalyDetectionService service = new AnomalyDetectionService(
                new SynFloodDetector(new FlowTrackingService(new FlowTable(10))),
                new PortScanDetector(),
                new DnsTunnelingDetector(),
                new AlertRingBuffer(10),
                slowPublisher,
                logger);

        List<Alert> alerts = List.of(alert(), alert(), alert());
        long start = System.nanoTime();
        service.raise(alerts);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMillis < 100, "raise() waited " + elapsedMillis + " ms on the publisher");
        assertEquals(3, service.getAlertRingBuffer().size(), "alerts are visible to the API immediately");

        service.close();

        verify(slowPublisher, times(3)).publish(any());
        verify(logger, times(3)).append(any());
    }

    @Test
    void fullQueueDropsAndCountsInsteadOfBlocking() {
        AlertPublisher blockedPublisher = mock(AlertPublisher.class);
        doAnswer(invocation -> {
            Thread.sleep(2_000);
            return null;
        }).when(blockedPublisher).publish(any());

        AsyncAlertDispatcher dispatcher = new AsyncAlertDispatcher(blockedPublisher, null, 2);
        for (int i = 0; i < 10; i++) {
            dispatcher.dispatch(alert());
        }

        assertTrue(dispatcher.getAlertsDropped() >= 7, "dropped " + dispatcher.getAlertsDropped());
    }

    private static Alert alert() {
        return new Alert(AlertType.PORT_SCAN, "198.51.100.5", "test alert", 25.0, Instant.now());
    }
}
