

Write-Host "Starting Sift (default profile, replaying bundled sample pcap)..."
Write-Host ""
Write-Host "Once you see 'Started SiftApplication' below, open a second"
Write-Host "PowerShell window and run:"
Write-Host ""
Write-Host '  Invoke-RestMethod "http://localhost:8080/api/alerts?limit=20" | ConvertTo-Json -Depth 5'
Write-Host '  Invoke-RestMethod "http://localhost:8080/api/flows/top?by=packetRate&limit=10" | ConvertTo-Json -Depth 5'
Write-Host ""
Write-Host "Dashboards, if the supporting services are up:"
Write-Host "  Grafana:    http://localhost:3000  (admin / admin)"
Write-Host "  Prometheus: http://localhost:9090"
Write-Host "  RabbitMQ:   http://localhost:15672 (guest / guest, unless changed)"
Write-Host ""

mvn spring-boot:run