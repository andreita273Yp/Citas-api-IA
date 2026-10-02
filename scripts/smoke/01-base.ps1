$ErrorActionPreference = 'Stop'
$api = 'http://localhost:8080/api/v1'
$tomorrow = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')

function Call($method, $path, $token, $body) {
  $h = @{}; if ($token) { $h.Authorization = "Bearer $token" }
  try {
    $args = @{ Method = $method; Uri = "$api$path"; Headers = $h; ContentType = 'application/json; charset=utf-8' }
    if ($null -ne $body) { $args.Body = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Compress -Depth 6)) }
    $r = Invoke-WebRequest -UseBasicParsing @args
    $text = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
    return [pscustomobject]@{ Status = [int]$r.StatusCode; Body = $(if ($text) { $text | ConvertFrom-Json } else { $null }) }
  } catch {
    $resp = $_.Exception.Response
    if (-not $resp) { throw }
    $text = (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd()
    return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = $text }
  }
}
function Login($email) { (Call POST '/auth/login' $null @{ email = $email; password = 'Demo1234*' }).Body.accessToken }
function Check($name, $actual, $expected) {
  $ok = "$actual" -eq "$expected"
  "{0,-58} {1,-6} (esperado {2}, obtenido {3})" -f $name, $(if ($ok) { 'PASS' } else { 'FAIL' }), $expected, $actual
}

$admin = Login 'admin@demo.invalid'
$p1 = Login 'paciente1@demo.invalid'
$p2 = Login 'paciente2@demo.invalid'
$p3 = Login 'paciente3@demo.invalid'
$prof1 = Login 'andrea.ruiz@demo.invalid'
Check 'Login de los 5 usuarios demo' (@($admin,$p1,$p2,$p3,$prof1) | Where-Object { $_ }).Count 5

$r = Call GET '/catalogs/locations' $p1; Check 'Catálogo de sedes' $r.Body.Count 2
$r = Call GET '/catalogs/roles' $p1; Check 'Catálogo de roles' $r.Body.Count 3
$r = Call GET '/catalogs/specialties' $p1; Check 'Especialidades activas' $r.Body.Count 12
$r = Call GET '/catalogs/professionals?specialtyId=1&locationId=1' $p1; Check 'Profesionales de Medicina General en HIC' $r.Body.Count 2
$r = Call GET "/availability?professionalId=1&locationId=1&specialtyId=1&date=$tomorrow" $p1
Check 'Slots libres de PROF-001 mañana en HIC (14 - 1 ocupado)' $r.Body.Count 13

$r = Call GET '/appointments' $p1; Check 'Paciente 1 ve su cita demo' $r.Body.Count 1
$appt1 = $r.Body[0]; Check 'Cita demo 1 está APPROVED' $appt1.status 'APPROVED'
$r = Call GET "/appointments/$($appt1.id)/history" $p1; Check 'Historial de la cita 1' $r.Body[0].newStatus 'APPROVED'
$r = Call GET "/appointments/$($appt1.id)" $p2; Check 'Paciente 2 NO ve la cita del paciente 1' $r.Status 404

$r = Call POST '/appointments' $p2 @{ professionalId = 1; locationId = 1; specialtyId = 1; startAt = "${tomorrow}T09:00"; reason = 'Prueba humo' }
Check 'Cita general nueva queda APPROVED' $r.Body.status 'APPROVED'
$newId = $r.Body.id
$r = Call POST '/appointments' $p3 @{ professionalId = 1; locationId = 1; specialtyId = 1; startAt = "${tomorrow}T09:00"; reason = 'Doble reserva' }
Check 'Mismo horario para otro paciente → 409' $r.Status 409

$r = Call GET '/admin/inbox' $admin
Check 'Bandeja admin: 1 especializada + 1 reprogramación' $r.Body.Count 2
$r = Call GET '/admin/inbox' $p1; Check 'Paciente NO entra a la bandeja admin' $r.Status 403
$r = Call POST '/admin/reschedule-requests/1/decision' $admin @{ decision = 'REJECT' }
Check 'Rechazar reprogramación sin motivo → 400' $r.Status 400
$r = Call POST '/admin/reschedule-requests/1/decision' $admin @{ decision = 'REJECT'; reason = 'Sin cupo' }
Check 'Rechazar reprogramación con motivo' $r.Body.status 'REJECTED'
$r = Call GET "/appointments/$($appt1.id)" $p1
Check 'Tras rechazo la cita 1 conserva su horario' ([datetime]$r.Body.startsAt).ToString('HH:mm') '08:00'

$r = Call POST "/appointments/$newId/reschedule-requests" $p2 @{ startAt = "${tomorrow}T10:00"; locationId = '1' }
Check 'Solicitar reprogramación queda PENDING' $r.Body.status 'PENDING'
$reqId = $r.Body.id
$r = Call GET "/availability?professionalId=1&locationId=1&specialtyId=1&date=$tomorrow" $p1
Check 'La franja 10:00 queda retenida (no disponible)' (@($r.Body | Where-Object { $_.startAt -like '*T10:00*' }).Count) 0
$r = Call POST "/admin/reschedule-requests/$reqId/decision" $admin @{ decision = 'APPROVE' }
Check 'Aprobar reprogramación' $r.Body.status 'APPROVED'
$r = Call GET "/appointments/$newId" $p2
Check 'La cita quedó a las 10:00' ([datetime]$r.Body.startsAt).ToString('HH:mm') '10:00'
$r = Call GET "/availability?professionalId=1&locationId=1&specialtyId=1&date=$tomorrow" $p1
Check 'La franja 09:00 quedó libre de nuevo' (@($r.Body | Where-Object { $_.startAt -like '*T09:00*' }).Count) 1

$r = Call GET '/professional/appointments' $prof1
Check 'Agenda de PROF-001 (cita demo + cita nueva)' $r.Body.Count 2
$r = Call GET '/professional/appointments' $p1; Check 'Paciente NO ve agenda profesional' $r.Status 403
$r = Call POST "/professional/appointments/$newId/closure" $prof1 @{ status = 'COMPLETED' }
Check 'No se cierra una cita que no ha terminado → 409' $r.Status 409

$r = Call POST "/appointments/$($appt1.id)/cancel" $p1 $null
Check 'Paciente 1 cancela su cita' $r.Body.status 'CANCELLED'
$r = Call GET "/appointments/$($appt1.id)/history" $p1
Check 'Historial registra CANCELLED con estado anterior APPROVED' "$($r.Body[-1].previousStatus)->$($r.Body[-1].newStatus)" 'APPROVED->CANCELLED'
$r = Call GET "/appointments/$($appt1.id)/history" $admin; Check 'Admin puede ver cualquier historial' $r.Status 200

# --- Fase 2: oferta de atención (ADMIN)
$r = Call GET '/admin/professionals' $admin; Check 'Admin lista los 8 profesionales demo' $r.Body.Count 8
$r = Call GET '/admin/professionals' $p1; Check 'Paciente NO lista profesionales admin' $r.Status 403
$r = Call GET '/admin/specialties' $admin; Check 'Admin lista las 12 especialidades' $r.Body.Count 12
$r = Call POST '/admin/specialties' $admin @{ code = 'DERMATOLOGIA'; name = 'Dermatología'; durationMinutes = 60 }; Check 'Admin crea especialidad de 60 min' $r.Status 201
$derm = $r.Body.id
$r = Call POST '/admin/specialties' $admin @{ code = 'MALA'; name = 'Mala'; durationMinutes = 45 }; Check 'Duración 45 min rechazada' $r.Status 400
$r = Call POST '/admin/professionals' $admin @{ firstName = 'Prof'; lastName = 'Nuevo'; documentType = 'CC'; documentNumber = '930000001'; email = 'prof.nuevo@demo.invalid'; phone = '3000000999'; initialPassword = 'Demo1234*'; professionalCode = 'PROF-009'; licenseNumber = 'RM-DEMO-0009' }
Check 'Admin crea profesional nuevo' $r.Status 201
$newProf = $r.Body.id
$r = Call PUT "/admin/professionals/$newProf/specialties" $admin @{ assignments = @(@{ specialtyId = $derm; primary = $true }) }; Check 'Asigna especialidad primaria' $r.Body.specialties[0].primary 'True'
$r = Call PUT "/admin/professionals/$newProf/locations" $admin @{ locationIds = @(1, 2) }; Check 'Asigna ambas sedes' $r.Body.locations.Count 2
$r = Call GET "/catalogs/professionals?specialtyId=$derm&locationId=2" $p1; Check 'Paciente ve al nuevo profesional en ICV' $r.Body.Count 1
$prof9 = Login 'prof.nuevo@demo.invalid'; Check 'El profesional nuevo inicia sesión' ([bool]$prof9) 'True'
$r = Call PATCH "/admin/professionals/$newProf" $admin @{ active = $false }; Check 'Admin desactiva profesional' $r.Body.active 'False'
$r = Call GET "/catalogs/professionals?specialtyId=$derm&locationId=2" $p1; Check 'Profesional inactivo ya no se ofrece' (@($r.Body).Count) 0