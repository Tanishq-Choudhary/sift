package com.sift.parsing;

import com.sift.model.Packet;
import com.sift.model.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

public class PacketParser {

    private static final Logger logger = LoggerFactory.getLogger(PacketParser.class);
    private static final int DNS_PORT = 53;

    public Optional<Packet> parse(byte[] rawBytes, Instant captureTime) {
        try {
            EthernetHeader ethernetHeader = EthernetParser.parse(rawBytes);
            if (ethernetHeader.etherType() != EthernetHeader.ETHER_TYPE_IPV4) {
                logger.debug("skipping non-ipv4 ethertype 0x{}", Integer.toHexString(ethernetHeader.etherType()));
                return Optional.empty();
            }

            Ipv4Header ipv4Header = Ipv4Parser.parse(rawBytes, EthernetHeader.HEADER_LENGTH);
            int transportOffset = EthernetHeader.HEADER_LENGTH + ipv4Header.headerLength();
            Protocol protocol = Protocol.fromIpProtocolNumber(ipv4Header.protocolNumber());

            Packet packet = switch (protocol) {
                case TCP -> parseTcpPacket(rawBytes, ipv4Header, transportOffset, captureTime);
                case UDP -> parseUdpPacket(rawBytes, ipv4Header, transportOffset, captureTime);
                case OTHER -> parseOtherProtocolPacket(ipv4Header, captureTime);
            };
            return Optional.of(packet);
        } catch (PacketParseException e) {
            logger.debug("skipping malformed packet: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Packet parseTcpPacket(byte[] rawBytes, Ipv4Header ipv4Header, int transportOffset, Instant captureTime) {
        TcpHeader tcpHeader = TcpParser.parse(rawBytes, transportOffset);
        int payloadLen = ipv4Header.totalLength() - ipv4Header.headerLength() - tcpHeader.headerLength();
        return new Packet(ipv4Header.sourceIp(), ipv4Header.destinationIp(), tcpHeader.sourcePort(),
                tcpHeader.destinationPort(), Protocol.TCP, tcpHeader.flags(), payloadLen, captureTime, null);
    }

    private Packet parseUdpPacket(byte[] rawBytes, Ipv4Header ipv4Header, int transportOffset, Instant captureTime) {
        UdpHeader udpHeader = UdpParser.parse(rawBytes, transportOffset);
        int payloadLen = udpHeader.length() - UdpHeader.HEADER_LENGTH;

        String dnsQueryName = null;
        if (udpHeader.sourcePort() == DNS_PORT || udpHeader.destinationPort() == DNS_PORT) {
            int dnsOffset = transportOffset + UdpHeader.HEADER_LENGTH;
            int dnsLimit = transportOffset + udpHeader.length();
            dnsQueryName = DnsQuestionParser.parseQueryName(rawBytes, dnsOffset, dnsLimit).orElse(null);
        }

        return new Packet(ipv4Header.sourceIp(), ipv4Header.destinationIp(), udpHeader.sourcePort(),
                udpHeader.destinationPort(), Protocol.UDP, null, payloadLen, captureTime, dnsQueryName);
    }

    private Packet parseOtherProtocolPacket(Ipv4Header ipv4Header, Instant captureTime) {
        int payloadLen = ipv4Header.totalLength() - ipv4Header.headerLength();
        return new Packet(ipv4Header.sourceIp(), ipv4Header.destinationIp(), Packet.NO_PORT, Packet.NO_PORT,
                Protocol.OTHER, null, payloadLen, captureTime, null);
    }
}