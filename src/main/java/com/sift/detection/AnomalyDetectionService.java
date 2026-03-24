package com.sift.detection;

import com.sift.alerting.AlertPublisher;
import com.sift.model.Alert;
import com.sift.model.Packet;

import java.util.ArrayList;
import java.util.List;

public class AnomalyDetectionService {

    private final SynFloodDetector synFloodDetector;
    private final PortScanDetector portScanDetector;
    private final DnsTunnelingDetector dnsTunnelingDetector;
    private final AlertRingBuffer alertRingBuffer;
    private final AlertPublisher alertPublisher;
    private final AlertJsonLineLogger alertJsonLineLogger;

    public AnomalyDetectionService(SynFloodDetector synFloodDetector,
                                    PortScanDetector portScanDetector,
                                    DnsTunnelingDetector dnsTunnelingDetector,
                                    AlertRingBuffer alertRingBuffer,
                                    AlertPublisher alertPublisher,
                                    AlertJsonLineLogger alertJsonLineLogger) {
        this.synFloodDetector = synFloodDetector;
        this.portScanDetector = portScanDetector;
        this.dnsTunnelingDetector = dnsTunnelingDetector;
        this.alertRingBuffer = alertRingBuffer;
        this.alertPublisher = alertPublisher;
        this.alertJsonLineLogger = alertJsonLineLogger;
    }

    public List<Alert> evaluate(Packet packet) {
        List<Alert> triggeredAlerts = new ArrayList<>();
        synFloodDetector.check(packet).ifPresent(triggeredAlerts::add);
        portScanDetector.check(packet).ifPresent(triggeredAlerts::add);
        dnsTunnelingDetector.check(packet).ifPresent(triggeredAlerts::add);

        for (Alert alert : triggeredAlerts) {
            alertRingBuffer.add(alert);
            alertPublisher.publish(alert);
            if (alertJsonLineLogger != null) {
                alertJsonLineLogger.append(alert);
            }
        }

        return triggeredAlerts;
    }

    public AlertRingBuffer getAlertRingBuffer() {
        return alertRingBuffer;
    }
}