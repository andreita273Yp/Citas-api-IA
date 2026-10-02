
# --- Fase 3: agenda, búsqueda, reserva y decisión
$d5 = (Get-Date).AddDays(5).ToString('yyyy-MM-dd')
$r = Call GET '/professional/me' $prof1; Check 'Profesional consulta su perfil (2 sedes)' $r.Body.locations.Count 2
$r = Call POST '/professional/blocks' $prof1 @{ locationId = 2; date = $d5; startTime = '08:00'; endTime = '10:00' }; Check 'Profesional publica bloque de 2 h' $r.Body.slots 4
$block = $r.Body.id
$r = Call POST '/professional/blocks' $prof1 @{ locationId = 2; date = $d5; startTime = '09:00'; endTime = '11:00' }; Check 'Bloque solapado rechazado' $r.Status 409
$r = Call POST '/professional/blocks' $prof1 @{ locationId = 2; date = (Get-Date).AddDays(-1).ToString('yyyy-MM-dd'); startTime = '08:00'; endTime = '09:00' }; Check 'Bloque en el pasado rechazado' $r.Status 400
$r = Call POST '/professional/blocks' $p1 @{ locationId = 2; date = $d5; startTime = '12:00'; endTime = '13:00' }; Check 'Paciente NO publica bloques' $r.Status 403
$r = Call GET "/availability?specialtyId=1&date=$d5&locationId=2" $p3; Check 'Paciente ve los 4 horarios nuevos en ICV' $r.Body.Count 4
$r = Call POST '/appointments' $p3 @{ professionalId = 1; locationId = 2; specialtyId = 1; startAt = "${d5}T08:00" }; Check 'Cita general en el bloque nuevo: APPROVED' $r.Body.status 'APPROVED'
$r = Call PUT "/professional/blocks/$block" $prof1 @{ locationId = 2; date = $d5; startTime = '08:00'; endTime = '12:00' }; Check 'No se edita bloque con cita' $r.Status 409
$r = Call POST '/appointments' $prof1 @{ professionalId = 1; locationId = 2; specialtyId = 1; startAt = "${d5}T08:30" }; Check 'Profesional NO reserva como paciente' $r.Status 403
$d2 = (Get-Date).AddDays(2).ToString('yyyy-MM-dd')
$r = Call GET "/availability?specialtyId=2&date=$d2&professionalId=3" $p3; Check 'Cardiología demo: 13 horarios (1 retenido)' $r.Body.Count 13
$r = Call POST '/appointments' $p3 @{ professionalId = 3; locationId = 1; specialtyId = 2; startAt = "${d2}T10:00"; reason = 'Control' }; Check 'Cita especializada queda REQUESTED' $r.Body.status 'REQUESTED'
$spec = $r.Body.id
$r = Call POST "/admin/appointments/$spec/decision" $admin @{ decision = 'REJECT' }; Check 'Rechazo sin motivo devuelve 400' $r.Status 400
$r = Call POST "/admin/appointments/$spec/decision" $admin @{ decision = 'REJECT'; reason = 'Falta remisión' }; Check 'Admin rechaza con motivo' $r.Body.status 'REJECTED'
$r = Call GET "/appointments/$spec" $p3; Check 'Paciente ve el motivo del rechazo' $r.Body.decisionReason 'Falta remisión'
$r = Call POST '/admin/appointments/2/decision' $admin @{ decision = 'APPROVE' }; Check 'Admin aprueba la solicitud demo' $r.Body.status 'APPROVED'
$r = Call POST '/admin/appointments/2/decision' $prof1 @{ decision = 'APPROVE' }; Check 'Profesional NO resuelve solicitudes' $r.Status 403
