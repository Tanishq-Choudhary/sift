package com.sift.detection;

import com.sift.alerting.AlertPublisher;
import com.sift.alerting.AsyncAlertDispatcher;
import com.sift.model.Alert;
import com.sift.model.Packet;

import java.util.ArrayList;
import java.util.List;

public class AnomalyDetectionService implements AutoCloseable {

    private final SynFloodDetector synFloodDetector;
    private final PortScanDetector portScanDetector;
    private final DnsTunnelingDetector dnsTunnelingDetector;
    private final AlertRingBuffer alertRingBuffer;
    private final AsyncAlertDispatcher alertDispatcher;

    public AnomalyDetectionService(SynFloodDetector synFloodDetector,
                                    PortScanDetector portScanDetector,
                                    DnsTunnelingDetector dnsTunnelingDetector,
                                    AlertRingBuffer alertRingBuffer,
                                    AlertPublisher alertPublisher,
                                    AlertJsonLineLogger alertJsonLineLogger) {
        this(synFloodDetector, portScanDetector, dnsTunnelingDetector, alertRingBuffer,
                new AsyncAlertDispatcher(alertPublisher, alertJsonLineLogger));
    }

    public AnomalyDetectionService(SynFloodDetector synFloodDetector,
                                    PortScanDetector portScanDetector,
                                    DnsTunnelingDetector dnsTunnelingDetector,
                                    AlertRingBuffer alertRingBuffer,
                                    AsyncAlertDispatcher alertDispatcher) {
        this.synFloodDetector = synFloodDetector;
        this.portScanDetector = portScanDetector;
        this.dnsTunnelingDetector = dnsTunnelingDetector;
        this.alertRingBuffer = alertRingBuffer;
        this.alertDispatcher = alertDispatcher;
    }

    public List<Alert> detect(Packet packet) {
        List<Alert> triggeredAlerts = new ArrayList<>();
        synFloodDetector.check(packet).ifPresent(triggeredAlerts::add);
        portScanDetector.check(packet).ifPresent(triggeredAlerts::add);
        dnsTunnelingDetector.check(packet).ifPresent(triggeredAlerts::add);
        return triggeredAlerts;
    }

    public void raise(List<Alert> alerts) {
        for (Alert alert : alerts) {
            alertRingBuffer.add(alert);
            alertDispatcher.dispatch(alert);
        }
    }

    public List<Alert> evaluate(Packet packet) {
        List<Alert> triggeredAlerts = detect(packet);
        raise(triggeredAlerts);
        return triggeredAlerts;
    }

    public AlertRingBuffer getAlertRingBuffer() {
        return alertRingBuffer;
    }

    public AsyncAlertDispatcher getAlertDispatcher() {
        return alertDispatcher;
    }

    @Override
    public void close() {
        alertDispatcher.close();
    }
}
