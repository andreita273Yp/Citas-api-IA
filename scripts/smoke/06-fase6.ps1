
# --- Fase 6: agenda profesional, cierre, bandeja y auditoría
$r = Call GET "/professional/appointments?date=$tomorrow&view=DAY" $prof1; Check 'Agenda del día incluye la cita reprogramada' (@($r.Body | Where-Object { $_.id -eq $newId }).Count) 1
Check 'Agenda: cita futura aún no se puede cerrar' (@($r.Body | Where-Object { $_.closable }).Count) 0
Check 'Agenda: del paciente solo el nombre (sin correo)' ([bool]($r.Body[0].patient -and -not $r.Body[0].patientEmail)) 'True'
$r = Call GET "/professional/appointments?date=$tomorrow&view=WEEK" $prof1; Check 'Vista semanal responde' $r.Status 200
$r = Call GET "/professional/appointments?date=$tomorrow&view=DAY&locationId=2" $prof1; Check 'Filtro por sede ICV excluye la cita de HIC' (@($r.Body | Where-Object { $_.id -eq $newId }).Count) 0
$r = Call GET "/professional/appointments?date=$tomorrow&view=MONTH" $prof1; Check 'Vista desconocida → 400' $r.Status 400
$r = Call POST "/professional/appointments/$newId/closure" $prof1 @{ status = 'CANCELLED' }; Check 'Cierre con estado inválido → 400' $r.Status 400
$prof2 = Login 'carlos.mejia@demo.invalid'
$r = Call POST "/professional/appointments/$newId/closure" $prof2 @{ status = 'COMPLETED' }; Check 'Otro profesional no cierra la cita → 404' $r.Status 404
$r = Call GET '/admin/inbox' $admin; $all = @($r.Body).Count; Check 'Bandeja ADMIN responde' $r.Status 200
$r = Call GET '/admin/inbox?professionalId=999' $admin; Check 'Filtro por profesional inexistente → vacío' @($r.Body).Count 0
$r = Call GET '/admin/inbox?locationId=1' $admin; Check 'Filtro por sede no amplía la bandeja' ([bool](@($r.Body).Count -le $all)) 'True'
$r = Call GET '/admin/inbox' $prof1; Check 'Profesional NO entra a la bandeja' $r.Status 403
$r = Call GET "/appointments/$newId/history" $prof1; Check 'Profesional asignado ve el historial' $r.Status 200
$r = Call GET "/appointments/$newId/history" $prof2; Check 'Profesional ajeno NO ve el historial → 404' $r.Status 404
$r = Call GET "/appointments/$newId/history" $p1; Check 'Paciente ajeno NO ve el historial → 404' $r.Status 404
$r = Call DELETE "/appointments/$newId/history" $admin $null; Check 'El historial no se borra por API → 405' $r.Status 405
