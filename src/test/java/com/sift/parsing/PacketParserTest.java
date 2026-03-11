package com.sift.parsing;

import com.sift.model.Packet;
import com.sift.model.Protocol;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PacketParserTest {

    private final PacketParser parser = new PacketParser();

    @Test
    void parsesTcpSynPacketWithCorrectFields() {
        byte[] raw = buildTcpSynPacket("10.0.0.10", "10.0.0.20", 51234, 80, 1000L);

        Optional<Packet> result = parser.parse(raw, Instant.now());

        assertThat(result).isPresent();
        Packet packet = result.get();
        assertThat(packet.srcIp()).isEqualTo("10.0.0.10");
        assertThat(packet.dstIp()).isEqualTo("10.0.0.20");
        assertThat(packet.srcPort()).isEqualTo(51234);
        assertThat(packet.dstPort()).isEqualTo(80);
        assertThat(packet.protocol()).isEqualTo(Protocol.TCP);
        assertThat(packet.tcpFlags().syn()).isTrue();
        assertThat(packet.tcpFlags().ack()).isFalse();
        assertThat(packet.tcpFlags().fin()).isFalse();
        assertThat(packet.tcpFlags().rst()).isFalse();
    }

    @Test
    void parsesDnsQueryNameWithMultipleLabels() {
        byte[] raw = buildDnsQueryPacket("10.0.0.10", "10.0.0.1", 52000, "mail.google.com");

        Optional<Packet> result = parser.parse(raw, Instant.now());

        assertThat(result).isPresent();
        Packet packet = result.get();
        assertThat(packet.protocol()).isEqualTo(Protocol.UDP);
        assertThat(packet.dstPort()).isEqualTo(53);
        assertThat(packet.dnsQueryName()).isEqualTo("mail.google.com");
    }

    @Test
    void skipsTruncatedPacketInsteadOfThrowing() {
        byte[] raw = buildTcpSynPacket("10.0.0.10", "10.0.0.20", 51234, 80, 1000L);
        byte[] truncated = new byte[20];
        System.arraycopy(raw, 0, truncated, 0, truncated.length);

        Optional<Packet> result = parser.parse(truncated, Instant.now());

        assertThat(result).isEmpty();
    }

    @Test
    void skipsNonIpv4EtherType() {
        byte[] raw = new byte[14];
        raw[12] = (byte) 0x86;
        raw[13] = (byte) 0xDD;

        Optional<Packet> result = parser.parse(raw, Instant.now());

        assertThat(result).isEmpty();
    }

    private static byte[] buildTcpSynPacket(String srcIp, String dstIp, int srcPort, int dstPort, long seq) {
        ByteBuffer buffer = ByteBuffer.allocate(14 + 20 + 20);

        buffer.put(mac(0x01)).put(mac(0x02)).putShort((short) 0x0800);

        buffer.put((byte) 0x45).put((byte) 0x00).putShort((short) 40);
        buffer.putShort((short) 0).putShort((short) 0);
        buffer.put((byte) 64).put((byte) 6).putShort((short) 0);
        buffer.put(ipBytes(srcIp)).put(ipBytes(dstIp));

        buffer.putShort((short) srcPort).putShort((short) dstPort);
        buffer.putInt((int) seq).putInt(0);
        buffer.put((byte) 0x50).put((byte) 0x02).putShort((short) 0xFFFF);
        buffer.putShort((short) 0).putShort((short) 0);

        return buffer.array();
    }

    private static byte[] buildDnsQueryPacket(String srcIp, String dstIp, int srcPort, String domain) {
        byte[] question = encodeDnsQuestion(domain);
        int dnsLength = 12 + question.length;
        int udpLength = 8 + dnsLength;
        int ipTotalLength = 20 + udpLength;

        ByteBuffer buffer = ByteBuffer.allocate(14 + ipTotalLength);

        buffer.put(mac(0x01)).put(mac(0x02)).putShort((short) 0x0800);

        buffer.put((byte) 0x45).put((byte) 0x00).putShort((short) ipTotalLength);
        buffer.putShort((short) 0).putShort((short) 0);
        buffer.put((byte) 64).put((byte) 17).putShort((short) 0);
        buffer.put(ipBytes(srcIp)).put(ipBytes(dstIp));

        buffer.putShort((short) srcPort).putShort((short) 53);
        buffer.putShort((short) udpLength).putShort((short) 0);

        buffer.putShort((short) 0x1234);
        buffer.putShort((short) 0x0100);
        buffer.putShort((short) 1);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.put(question);

        return buffer.array();
    }

    private static byte[] encodeDnsQuestion(String domain) {
        String[] labels = domain.split("\\.");
        ByteBuffer buffer = ByteBuffer.allocate(domain.length() + 2 + 5);
        for (String label : labels) {
            byte[] labelBytes = label.getBytes(StandardCharsets.US_ASCII);
            buffer.put((byte) labelBytes.length).put(labelBytes);
        }
        buffer.put((byte) 0);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);

        byte[] result = new byte[buffer.position()];
        buffer.flip();
        buffer.get(result);
        return result;
    }

    private static byte[] mac(int lastByte) {
        return new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, 0x00, 0x00, (byte) lastByte};
    }

    private static byte[] ipBytes(String ip) {
        String[] parts = ip.split("\\.");
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) Integer.parseInt(parts[i]);
        }
        return result;
    }
}