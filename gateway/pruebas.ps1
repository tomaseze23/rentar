function Login($email, $pass) {
  $body = @{ email = $email; password = $pass } | ConvertTo-Json
  (Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType 'application/json' -Body $body).token
}
$token = Login 'admin@rentar.com' 'admin1234'
$h = @{ Authorization = "Bearer $token" }
$base = 'http://localhost:8080/api/vehiculos'
$bc = 'http://localhost:8080/api/clientes'
$br = 'http://localhost:8080/api/reservas'