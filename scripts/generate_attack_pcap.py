#!/usr/bin/env python3
import argparse
from pathlib import Path

from scapy.all import DNS, DNSQR, IP, TCP, UDP, Ether, wrpcap

CLIENT_MAC = "aa:bb:cc:00:00:01"
SERVER_MAC = "aa:bb:cc:00:00:02"


def tcp_flow(client_ip, server_ip, client_port, server_port, payload):
    packets = []
    base_seq = 1000
    packets.append(
        Ether(src=CLIENT_MAC, dst=SERVER_MAC)
        / IP(src=client_ip, dst=server_ip)
        / TCP(sport=client_port, dport=server_port, flags="S", seq=base_seq)
    )
    packets.append(
        Ether(src=SERVER_MAC, dst=CLIENT_MAC)
        / IP(src=server_ip, dst=client_ip)
        / TCP(sport=server_port, dport=client_port, flags="SA", seq=5000, ack=base_seq + 1)
    )
    packets.append(
        Ether(src=CLIENT_MAC, dst=SERVER_MAC)
        / IP(src=client_ip, dst=server_ip)
        / TCP(sport=client_port, dport=server_port, flags="A", seq=base_seq + 1, ack=5001)
        / payload
    )
    packets.append(
        Ether(src=SERVER_MAC, dst=CLIENT_MAC)
        / IP(src=server_ip, dst=client_ip)
        / TCP(sport=server_port, dport=client_port, flags="A", seq=5001, ack=base_seq + 1 + len(payload))
    )
    packets.append(
        Ether(src=CLIENT_MAC, dst=SERVER_MAC)
        / IP(src=client_ip, dst=server_ip)
        / TCP(sport=client_port, dport=server_port, flags="FA", seq=base_seq + 1 + len(payload), ack=5001)
    )
    packets.append(
        Ether(src=SERVER_MAC, dst=CLIENT_MAC)
        / IP(src=server_ip, dst=client_ip)
        / TCP(sport=server_port, dport=client_port, flags="A", seq=5001, ack=base_seq + 2 + len(payload))
    )
    return packets


def build_normal_http_pcap(destination):
    packets = []
    client_ip = "10.0.0.10"
    server_ip = "10.0.0.20"
    client_port = 51000
    for i in range(20):
        payload = f"GET /page{i} HTTP/1.1\r\nHost: example.com\r\n\r\n"
        packets.extend(tcp_flow(client_ip, server_ip, client_port + i, 80, payload))
    wrpcap(str(destination), packets)


def build_dns_pcap(destination):
    packets = []
    client_ip = "10.0.0.10"
    resolver_ip = "10.0.0.1"
    domains = [
        "mail.google.com",
        "www.example.com",
        "api.github.com",
        "cdn.jsdelivr.net",
        "accounts.spotify.com",
    ]
    for i, domain in enumerate(domains * 4):
        packets.append(
            Ether(src=CLIENT_MAC, dst=SERVER_MAC)
            / IP(src=client_ip, dst=resolver_ip)
            / UDP(sport=52000 + i, dport=53)
            / DNS(rd=1, qd=DNSQR(qname=domain))
        )
    wrpcap(str(destination), packets)


def build_mixed_pcap(destination):
    packets = []
    packets.extend(tcp_flow("10.0.0.11", "10.0.0.21", 51500, 80, "GET /index.html HTTP/1.1\r\n\r\n"))
    packets.append(
        Ether(src=CLIENT_MAC, dst=SERVER_MAC)
        / IP(src="10.0.0.11", dst="10.0.0.1")
        / UDP(sport=53100, dport=53)
        / DNS(rd=1, qd=DNSQR(qname="mail.google.com"))
    )
    wrpcap(str(destination), packets)


def main():
    parser = argparse.ArgumentParser(description="Generate sample pcaps for Sift Phase 1")
    parser.add_argument("--output-dir", default="../src/main/resources/pcaps")
    args = parser.parse_args()

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    build_normal_http_pcap(output_dir / "normal_http_traffic.pcap")
    build_dns_pcap(output_dir / "dns_traffic.pcap")
    build_mixed_pcap(output_dir / "mixed_traffic.pcap")

    print(f"wrote sample pcaps to {output_dir.resolve()}")


if __name__ == "__main__":
    main()