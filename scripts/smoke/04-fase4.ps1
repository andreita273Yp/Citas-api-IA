
# --- Fase 4: recuperación, perfil, afiliación, EPS y planes
$r = Call POST '/auth/password-recovery' $null @{ email = 'paciente2@demo.invalid' }; Check 'Recuperación responde 202 genérico' $r.Status 202
$r = Call POST '/auth/password-recovery' $null @{ email = 'no-existe@demo.invalid' }; Check 'Email inexistente también 202' $r.Status 202
$r = Call GET '/admin/local-mailbox/password-recovery' $admin; Check 'Buzón local entrega el token al admin' $r.Body[0].email 'paciente2@demo.invalid'
$tok = $r.Body[0].token
$r = Call GET '/admin/local-mailbox/password-recovery' $p1; Check 'Paciente NO lee el buzón' $r.Status 403
$r = Call POST '/auth/password-reset' $null @{ token = $tok; password = 'Nueva1234*'; confirmation = 'Otra1234*' }; Check 'Confirmación distinta devuelve 400' $r.Status 400
$r = Call POST '/auth/password-reset' $null @{ token = $tok; password = 'Nueva1234*'; confirmation = 'Nueva1234*' }; Check 'Restablecer contraseña devuelve 204' $r.Status 204
$r = Call POST '/auth/password-reset' $null @{ token = $tok; password = 'Otra1234*x'; confirmation = 'Otra1234*x' }; Check 'Token reutilizado devuelve 401' $r.Status 401
$r = Call POST '/auth/login' $null @{ email = 'paciente2@demo.invalid'; password = 'Nueva1234*' }; Check 'Login con la contraseña nueva' $r.Status 200
$r = Call POST '/auth/login' $null @{ email = 'paciente2@demo.invalid'; password = 'Demo1234*' }; Check 'La contraseña anterior ya no sirve' $r.Status 401
$r = Call PATCH '/users/me' $p1 @{ phone = '+57 311 555 0000'; email = 'otro@demo.invalid' }; Check 'Perfil: cambia solo el teléfono' "$($r.Body.phone)|$($r.Body.email)" '+57 311 555 0000|paciente1@demo.invalid'
$r = Call PATCH '/users/me' $p1 @{ phone = 'abc' }; Check 'Teléfono inválido devuelve 400' $r.Status 400
$r = Call GET '/users/me/affiliation' $p1; Check 'Afiliación demo: régimen derivado del plan' "$($r.Body.epsName)|$($r.Body.regimeCode)" 'EPS Demo Salud|CONTRIBUTIVO'
$r = Call PUT '/users/me/affiliation' $p1 @{ planId = 3; membershipNumber = 'AF-NEW-100' }; Check 'Cambiar de plan' $r.Body.planId 3
$r = Call POST '/admin/eps' $admin @{ code = 'EPS_SMOKE'; name = 'EPS Smoke' }; Check 'Admin crea EPS' $r.Status 201
$epsId = $r.Body.id
$r = Call POST '/admin/eps-plans' $admin @{ epsId = $epsId; regimeId = 2; code = 'SMK'; name = 'Plan Smoke' }; Check 'Admin crea plan con régimen subsidiado' $r.Body.regimeCode 'SUBSIDIADO'
$planId = $r.Body.id
$r = Call GET "/catalogs/eps-plans?epsId=$epsId" $null; Check 'Plan visible en el catálogo público' (@($r.Body).Count) 1
$r = Call PATCH "/admin/eps-plans/$planId" $admin @{ active = $false }; Check 'Admin desactiva el plan' $r.Body.active 'False'
$r = Call GET "/catalogs/eps-plans?epsId=$epsId" $null; Check 'Plan inactivo ya no se ofrece' (@($r.Body).Count) 0
$r = Call PUT '/users/me/affiliation' $p1 @{ planId = $planId; membershipNumber = 'AF-X' }; Check 'No se afilia a un plan inactivo' $r.Status 400
$r = Call POST '/admin/eps' $p1 @{ code = 'HACK'; name = 'Hack' }; Check 'Paciente NO crea EPS' $r.Status 403
