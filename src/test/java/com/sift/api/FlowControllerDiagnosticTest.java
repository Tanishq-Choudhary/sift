package com.sift.api;

import com.sift.flow.FlowTable;
import com.sift.flow.FlowTrackingService;
import com.sift.model.Packet;
import com.sift.model.Protocol;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowControllerDiagnosticTest {

    @Test
    void snapshotSizeMatchesTableSizeImmediatelyAfterTracking() {
        FlowTable flowTable = new FlowTable(1000);
        FlowTrackingService flowTrackingService = new FlowTrackingService(flowTable);

        Packet packet = new Packet("10.0.0.11", "10.0.0.21", 51500, 80, Protocol.UDP,
                null, 0, Instant.now(), null);
        flowTrackingService.track(packet);

        System.out.println("flowTable.size() = " + flowTable.size());
        System.out.println("flowTable.snapshot().size() = " + flowTable.snapshot().size());
        assertEquals(flowTable.size(), flowTable.snapshot().size());
    }

    @Test
    void flowControllerReturnsAllTrackedFlowsRegardlessOfRate() {
        FlowTable flowTable = new FlowTable(1000);
        FlowTrackingService flowTrackingService = new FlowTrackingService(flowTable);

        Packet packet = new Packet("10.0.0.11", "10.0.0.21", 51500, 80, Protocol.UDP,
                null, 0, Instant.now().minusSeconds(30), null); // outside the 10s window on purpose
        flowTrackingService.track(packet);

        System.out.println("aged test: flowTable.size() = " + flowTable.size());
        System.out.println("aged test: flowTable.snapshot().size() = " + flowTable.snapshot().size());

        FlowController controller = new FlowController(flowTable);
        FlowController.FlowsResponse response = controller.getTopFlows("packetRate", 20);

        System.out.println("flows returned = " + response.flows().size());
        assertEquals(1, response.flows().size(), "flow should still be listed even at 0.0 rate once outside the window");
    }
}