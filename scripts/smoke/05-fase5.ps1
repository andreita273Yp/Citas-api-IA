
# --- Fase 5: mis citas, cancelación y reprogramación
$r = Call GET '/appointments?status=APPROVED' $p3; Check 'Paciente 3 filtra sus citas aprobadas' (@($r.Body | Where-Object { $_.status -ne 'APPROVED' }).Count) 0
$r = Call GET '/appointments' $p3; Check 'La lista trae sede, profesional y especialidad' ([bool]($r.Body[0].locationCode -and $r.Body[0].professional -and $r.Body[0].specialty)) 'True'
$rejectedId = ($r.Body | Where-Object { $_.status -eq 'REJECTED' } | Select-Object -First 1).id
$r = Call GET "/appointments/$rejectedId" $p3; Check 'Detalle muestra el motivo del rechazo' $r.Body.decisionReason 'Falta remisión'
$r = Call GET "/appointments/$rejectedId" $p1; Check 'Otro paciente NO ve esa cita' $r.Status 404
$d3 = (Get-Date).AddDays(3).ToString('yyyy-MM-dd')
$r = Call POST '/appointments' $p1 @{ professionalId = 1; locationId = 2; specialtyId = 1; startAt = "${d3}T08:30" }; Check 'Paciente 1 reserva una general' $r.Body.status 'APPROVED'
$mine = $r.Body.id
$r = Call POST "/appointments/$mine/reschedule-requests" $p1 @{ startAt = "${d3}T10:00"; locationId = 2 }; Check 'Solicita reprogramación (PENDING)' $r.Body.status 'PENDING'
$req = $r.Body.id
$r = Call GET "/appointments/$mine" $p1; Check 'La cita conserva su horario original' ([datetime]$r.Body.startsAt).ToString('HH:mm') '08:30'
$r = Call POST "/admin/reschedule-requests/$req/decision" $admin @{ decision = 'REJECT'; reason = 'Sin cupo' }; Check 'Admin rechaza la reprogramación' $r.Body.status 'REJECTED'
$r = Call POST "/appointments/$mine/reschedule-requests/$req/keep" $p1 $null; Check 'Paciente decide conservar la cita' $r.Status 204
$r = Call GET "/appointments/$mine" $p1; Check 'Queda registrada la decisión del paciente' $r.Body.reschedule.patientAction 'KEEP_APPOINTMENT'
$r = Call POST "/appointments/$mine/cancel" $p1 $null; Check 'Paciente cancela su cita' $r.Body.status 'CANCELLED'
$r = Call POST "/appointments/$mine/cancel" $p1 $null; Check 'Una cita cancelada no se vuelve a cancelar' $r.Status 409
