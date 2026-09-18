# Backlog de historias de usuario

Formato por HU: estado inicial, esfuerzo cualitativo, trazabilidad, dependencias, criterios verificables, tareas necesarias y DoD específica. Se aplica además el DoD común de [[README]]. Ninguna historia está aprobada.

## E01 — Identidad y acceso

### HU-001 — Definir identidad persistente y contexto de roles

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-01, RF-02, 3FN usuarios/roles/tokens · **Dependencias:** ninguna.

Como equipo, quiero un modelo de identidad normalizado para que los actores y sus credenciales se representen sin datos redundantes.

**Criterios de aceptación:**
1. Usuarios, roles y la relación de múltiples roles se justifican con claves, unicidades y cardinalidades, sin listas en columnas.
2. Email y tipo/número de documento tienen reglas de unicidad inequívocas; password solo admite hash adaptativo.
3. Refresh y recuperación se modelan como credenciales temporales revocables/consumibles sin almacenar el secreto en texto claro.

**Tareas técnicas:** documentar ER y dependencias funcionales; decidir hashes/índices; definir puertos de identidad; diseñar migración Flyway y seed de roles.

**DoD específica:** el diseño 1FN→3FN y la decisión de persistencia de tokens están enlazados, revisables y no requieren guardar secretos recuperables.

### HU-002 — Registrar una cuenta USER

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-01, seguridad mínima · **Dependencias:** [[historias#HU-001--Definir-identidad-persistente-y-contexto-de-roles|HU-001]].

Como visitante, quiero registrarme con mis datos mínimos ficticios para obtener una cuenta USER.

**Criterios de aceptación:**
1. Se exigen nombres, apellidos, tipo/número de documento, email, teléfono y contraseña válida.
2. Un email o documento existente se rechaza sin crear una cuenta parcial.
3. La cuenta creada tiene solo el rol USER inicial y la contraseña nunca aparece en respuesta, persistencia ni log.

**Tareas técnicas:** definir DTO/validadores y caso de uso; hashear contraseña; persistir transaccionalmente; documentar endpoint y errores.

**DoD específica:** existen pruebas de registro válido, duplicados y ausencia de exposición de password, más contrato REST enlazado.

### HU-003 — Iniciar, renovar y cerrar sesión JWT

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-02, seguridad mínima · **Dependencias:** [[historias#HU-002--Registrar-una-cuenta-USER|HU-002]].

Como actor registrado, quiero iniciar sesión, renovar mi acceso y cerrarlo para operar con una sesión segura.

**Criterios de aceptación:**
1. Credenciales válidas emiten access token de vida corta y refresh token distinguibles; los roles alimentan autorización.
2. Un refresh válido produce el resultado definido sin aceptar uno vencido, revocado o reutilizado fuera de la política documentada.
3. Logout revoca el refresh aplicable y bloquea renovaciones posteriores.

**Tareas técnicas:** definir política de expiración/revocación; adaptar Spring Security/JWT; implementar casos de uso y filtros; publicar contrato login/refresh/logout.

**DoD específica:** pruebas cubren éxito, contraseña incorrecta, refresh inválido/revocado y logout; secretos JWT vienen de environment y no se registran.

### HU-004 — Recuperar y cambiar contraseña

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-03 · **Dependencias:** [[historias#HU-001--Definir-identidad-persistente-y-contexto-de-roles|HU-001]], [[historias#HU-003--Iniciar-renovar-y-cerrar-sesión-JWT|HU-003]].

Como actor, quiero solicitar y usar una recuperación de contraseña de un solo uso para recuperar acceso sin exponer secretos.

**Criterios de aceptación:**
1. La solicitud genera un token temporal de un solo uso para una cuenta existente sin revelar información sensible.
2. En desarrollo, la entrega simulada se expone solo por mecanismo seguro y controlado; SMTP real no es requisito.
3. El cambio con token válido consume/invalida el token y actualiza el hash de contraseña; un token vencido o usado falla.

**Tareas técnicas:** definir expiración y almacenamiento seguro; caso de uso de solicitud/cambio; adaptar mecanismo de desarrollo; documentar controles de logging.

**DoD específica:** hay pruebas de token válido, vencido, consumido y de invalidación tras cambio, sin que token o password aparezcan en logs.

### HU-005 — Aplicar autorización por rol y ownership

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-02, RF-16, seguridad mínima · **Dependencias:** [[historias#HU-003--Iniciar-renovar-y-cerrar-sesión-JWT|HU-003]].

Como propietario del sistema, quiero que cada actor solo ejecute y vea recursos de su rol/propiedad.

**Criterios de aceptación:**
1. USER no puede operar recursos de otro USER, ADMIN ni PROFESSIONAL.
2. PROFESSIONAL solo puede acceder a su agenda y a datos de usuarios presentes en sus propias citas.
3. ADMIN tiene únicamente los privilegios administrativos documentados y toda denegación devuelve respuesta REST consistente.

**Tareas técnicas:** matriz rol-acción-recurso; políticas de ownership en aplicación; configuración de Spring Security; pruebas de autorización negativa.

**DoD específica:** la matriz de acceso se enlaza desde el contrato y pruebas demuestran al menos un acceso permitido y uno denegado por actor relevante.

### HU-006 — Especificar el contrato REST de identidad

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-20, restricciones REST JSON · **Dependencias:** [[historias#HU-002--Registrar-una-cuenta-USER|HU-002]]–[[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como equipo frontend/backend, quiero un contrato de identidad versionable para integrar sin BFF ni ambigüedades.

**Criterios de aceptación:**
1. Registro, login, refresh, logout y recuperación documentan entradas, salidas, códigos y errores.
2. El contrato explicita encabezados/autorización y nunca modela secretos en respuestas o ejemplos.
3. El cliente consume directamente `citas-api` por URL configurable de environment.

**Tareas técnicas:** crear especificación REST; acordar errores y seguridad CORS; enlazar decisiones desde wiki; preparar validación de contrato.

**DoD específica:** contrato revisable y ejemplos sintéticos disponibles; una verificación de compatibilidad backend/cliente queda definida para S3+.

## E02 — Perfil y catálogos

### HU-007 — Consultar y actualizar perfil USER

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-04 · **Dependencias:** [[historias#HU-002--Registrar-una-cuenta-USER|HU-002]], [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como USER, quiero consultar y modificar solo mis datos permitidos para mantener mi perfil.

**Criterios de aceptación:**
1. Un USER autenticado obtiene únicamente su propio perfil.
2. La actualización valida datos permitidos y conserva las unicidades de email/documento.
3. Campos de rol, password y datos de otros usuarios no se modifican por esta operación.

**Tareas técnicas:** definir proyección de perfil; caso de uso de actualización; validar ownership/unicidad; documentar contrato.

**DoD específica:** pruebas separan actualización propia, intento ajeno y conflicto de unicidad.

### HU-008 — Asociar afiliación EPS, plan y régimen

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-04, 3FN catálogos/afiliación · **Dependencias:** [[historias#HU-007--Consultar-y-actualizar-perfil-USER|HU-007]], [[historias#HU-009--Consultar-cat%C3%A1logos-fijos|HU-009]], [[historias#HU-010--Gestionar-EPS|HU-010]], [[historias#HU-011--Gestionar-planes-de-EPS|HU-011]].

Como USER, quiero asociar mi EPS, plan y régimen para registrar mi afiliación sin duplicar catálogos.

**Criterios de aceptación:**
1. La afiliación referencia EPS, plan y régimen por claves, no por nombres repetidos.
2. El plan seleccionado pertenece a la EPS seleccionada y los catálogos requeridos están activos.
3. El mismo USER no conserva duplicados de la misma combinación de afiliación.

**Tareas técnicas:** modelar relación e índices; validar compatibilidad EPS-plan; caso de uso y contrato; probar restricciones.

**DoD específica:** la justificación 3FN identifica dependencias funcionales y pruebas cubren combinación válida, plan ajeno y duplicado.

### HU-009 — Consultar catálogos fijos

**Estado:** Propuesta · **Esfuerzo:** Bajo · **Trazabilidad:** RF-05 · **Dependencias:** [[historias#HU-001--Definir-identidad-persistente-y-contexto-de-roles|HU-001]].

Como consumidor autorizado, quiero consultar roles, estados, regímenes y sedes fijas para usar valores consistentes.

**Criterios de aceptación:**
1. Roles, estados de cita/reprogramación, regímenes y las dos sedes del laboratorio se cargan por seed.
2. Las consultas ofrecen solo valores de catálogo y las operaciones de administración no los eliminan ni alteran arbitrariamente.
3. Las sedes coinciden con HIC e ICV definidos en el PRD.

**Tareas técnicas:** definir seeds Flyway y consultas de lectura; documentar códigos estables; proteger operaciones de escritura.

**DoD específica:** seeds idempotentes/revisables y prueba de lectura corroboran valores fijos sin datos reales.

### HU-010 — Gestionar EPS

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-06, 3FN catálogos · **Dependencias:** [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como ADMIN, quiero crear, consultar, actualizar y activar/desactivar EPS para mantener el catálogo configurable.

**Criterios de aceptación:**
1. Solo ADMIN puede gestionar EPS y los datos requeridos se validan.
2. Una EPS referenciada no se borra físicamente; puede desactivarse según la política.
3. Las consultas para nuevas asociaciones distinguen registros activos sin borrar el historial referenciado.

**Tareas técnicas:** caso de uso CRUD/activación; restricciones de FK; endpoint y errores; pruebas de referencia existente.

**DoD específica:** se prueba creación, edición, desactivación y rechazo de borrado físico referenciado.

### HU-011 — Gestionar planes de EPS

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-06, RF-04, 3FN catálogos · **Dependencias:** [[historias#HU-010--Gestionar-EPS|HU-010]].

Como ADMIN, quiero gestionar planes pertenecientes a una EPS para que las afiliaciones sean consistentes.

**Criterios de aceptación:**
1. Cada plan referencia exactamente una EPS y no se duplica dentro de ella según la regla documentada.
2. Solo ADMIN crea, edita o activa/desactiva planes.
3. Un plan referenciado no se elimina físicamente y una afiliación no puede asociarlo a otra EPS.

**Tareas técnicas:** modelar FK/unicidad; CRUD y activación; filtrar planes por EPS; pruebas de integridad.

**DoD específica:** las pruebas demuestran relación EPS-plan, inactivación segura e imposibilidad de combinación inválida.

### HU-012 — Gestionar especialidades y duración

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-06, RF-09, RN-08, 3FN catálogos · **Dependencias:** [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como ADMIN, quiero administrar especialidades activas y su duración estándar para configurar una oferta reservable.

**Criterios de aceptación:**
1. Solo ADMIN gestiona especialidades; una referenciada se activa/desactiva en vez de borrarse.
2. La duración válida es solo 30 o 60 minutos y es propiedad de la especialidad.
3. Medicina General puede identificarse de forma consistente para el flujo de cita general.

**Tareas técnicas:** definir código/únicos y duración; CRUD/activación; contrato de catálogo; preparar impacto en cálculo de slots.

**DoD específica:** pruebas cubren duración permitida/no permitida, inactivación y estabilidad del identificador de Medicina General.

## E03 — Profesionales y disponibilidad

### HU-013 — Crear y activar profesional

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-07, 3FN profesional como usuario especializado · **Dependencias:** [[historias#HU-001--Definir-identidad-persistente-y-contexto-de-roles|HU-001]], [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como ADMIN, quiero crear un usuario PROFESSIONAL con código y matrícula ficticios para habilitar la oferta asistencial.

**Criterios de aceptación:**
1. El profesional es un usuario con rol PROFESSIONAL y atributos especializados separados, con código y matrícula únicos ficticios.
2. Solo ADMIN puede crearlo, activarlo o desactivarlo.
3. Un profesional inactivo no puede publicar bloques ni aparecer como elegible para nuevas reservas.

**Tareas técnicas:** diseñar especialización usuario-profesional; definir unicidades; caso de uso administrativo; pruebas de activación.

**DoD específica:** modelo y pruebas evidencian datos sintéticos, unicidades y exclusión del profesional inactivo.

### HU-014 — Asignar especialidades al profesional

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-07, RN-08, 3FN N:M · **Dependencias:** [[historias#HU-012--Gestionar-especialidades-y-duración|HU-012]], [[historias#HU-013--Crear-y-activar-profesional|HU-013]].

Como ADMIN, quiero asignar una o más especialidades y una primaria a un profesional para definir lo que puede atender.

**Criterios de aceptación:**
1. La relación profesional-especialidad se representa como N:M sin lista embebida.
2. Solo una especialidad asignada puede estar marcada como primaria por profesional.
3. No se puede reservar una especialidad inactiva o no asociada al profesional.

**Tareas técnicas:** tabla puente y restricción de primaria; caso de uso de asignación; validación en reserva; pruebas de consistencia.

**DoD específica:** las pruebas cubren múltiples asignaciones, única primaria y rechazo de especialidad no asociada.

### HU-015 — Asignar sedes al profesional

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-07, RN-07, 3FN N:M · **Dependencias:** [[historias#HU-009--Consultar-cat%C3%A1logos-fijos|HU-009]], [[historias#HU-013--Crear-y-activar-profesional|HU-013]].

Como ADMIN, quiero asignar HIC y/o ICV a un profesional para limitar dónde puede publicar disponibilidad.

**Criterios de aceptación:**
1. Un profesional puede tener una o ambas sedes mediante relación N:M normalizada.
2. Solo ADMIN modifica las sedes asignadas.
3. La creación de bloques y las reservas rechazan una sede no asignada al profesional.

**Tareas técnicas:** tabla puente; administración de asignaciones; regla de aplicación para bloque/reserva; pruebas de sede no habilitada.

**DoD específica:** se demuestra que ambas sedes son válidas, que una no asignada falla y que no se guardan sedes como texto duplicado.

### HU-016 — Crear y mantener bloques futuros

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-08, RN-06–07 · **Dependencias:** [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]], [[historias#HU-013--Crear-y-activar-profesional|HU-013]], [[historias#HU-015--Asignar-sedes-al-profesional|HU-015]].

Como PROFESSIONAL, quiero crear, editar y eliminar mis bloques futuros por sede para publicar mi disponibilidad.

**Criterios de aceptación:**
1. Solo el profesional dueño puede gestionar sus bloques futuros y debe elegir una sede asignada.
2. No se aceptan bloques en el pasado ni bloques solapados del mismo profesional.
3. Un bloque futuro con citas comprometidas no se edita ni elimina si ello afecta la reserva existente.

**Tareas técnicas:** caso de uso y validaciones de intervalo; detección de solape; autorización de ownership; contrato/calendario; pruebas de compromiso.

**DoD específica:** pruebas cubren pasado, solape, sede ajena, ownership y bloqueo por cita comprometida.

### HU-017 — Discretizar bloques y preservar reservas de slots

**Estado:** Propuesta · **Esfuerzo:** Muy alto · **Trazabilidad:** RF-08–10, RN-01, RN-05, 3FN citas 30/60 min · **Dependencias:** [[historias#HU-012--Gestionar-especialidades-y-duración|HU-012]], [[historias#HU-016--Crear-y-mantener-bloques-futuros|HU-016]].

Como sistema, quiero derivar slots de 30 minutos y gestionar reservas/retenciones de forma consistente para impedir doble reserva.

**Criterios de aceptación:**
1. Un bloque se discretiza en slots de 30 minutos; una duración de 60 requiere dos slots consecutivos.
2. Una reserva o retención concurrente no puede adjudicar el mismo slot a dos operaciones.
3. El diseño explica claves, índices, transacción/bloqueo y cómo se liberan slots según el ciclo de vida.

**Tareas técnicas:** decidir representación de slots/reservas; definir índices y estrategia de concurrencia; migraciones; puertos de disponibilidad; pruebas de concurrencia relevantes.

**DoD específica:** decisión de no doble reserva enlazada, con pruebas de 30/60 minutos, consecutividad y contención concurrente.

### HU-018 — Consultar disponibilidad filtrada

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-10, RN-05, RN-08 · **Dependencias:** [[historias#HU-014--Asignar-especialidades-al-profesional|HU-014]], [[historias#HU-015--Asignar-sedes-al-profesional|HU-015]], [[historias#HU-017--Discretizar-bloques-y-preservar-reservas-de-slots|HU-017]].

Como USER, quiero filtrar disponibilidad por sede, tipo, especialidad, profesional y fecha para elegir un horario reservable.

**Criterios de aceptación:**
1. Los filtros producen ofertas solo de profesionales activos, habilitados en sede y asociados a especialidad activa.
2. La salida muestra únicamente inicios que completan la duración requerida, incluyendo dos slots consecutivos para 60 minutos.
3. No se muestran slots ya reservados o retenidos ni horarios pasados.

**Tareas técnicas:** consulta optimizada/indexada; criterios de filtro; proyección REST; documentar semántica de tipo general/especializada; pruebas de visibilidad.

**DoD específica:** evidencia de consultas para duración 30/60 y filtros combinados, sin exposición de slots no reservables.

### HU-019 — Documentar contrato de disponibilidad y agenda

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-08–10, RF-20 · **Dependencias:** [[historias#HU-016--Crear-y-mantener-bloques-futuros|HU-016]]–[[historias#HU-018--Consultar-disponibilidad-filtrada|HU-018]].

Como equipo, quiero un contrato REST de bloques, calendario y disponibilidad para integrar UI y backend con las mismas reglas.

**Criterios de aceptación:**
1. El contrato describe operaciones de bloques, filtros, formato de intervalos, duración y errores de solape/no disponibilidad.
2. Las respuestas no exponen datos de usuarios ni detalles internos de bloqueo.
3. La URL del backend y CORS están contemplados para consumo web directo.

**Tareas técnicas:** especificar endpoints/esquemas; enlazar regla de concurrencia; acordar errores; definir prueba de contrato.

**DoD específica:** una revisión cruzada puede derivar la pantalla de agenda/búsqueda sin inferir reglas fuera del contrato.

## E04 — Citas y ciclo de vida

### HU-020 — Confirmar cita general aprobada automáticamente

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-11, RN-01–02, RN-05, RN-08 · **Dependencias:** [[historias#HU-018--Consultar-disponibilidad-filtrada|HU-018]].

Como USER, quiero reservar Medicina General con un profesional disponible para obtener una cita aprobada sin intervención administrativa.

**Criterios de aceptación:**
1. El flujo solo permite la especialidad identificada como Medicina General y un profesional elegible en sede/fecha/hora.
2. La confirmación revalida disponibilidad de manera atómica y crea la cita `APPROVED` solo si la franja sigue libre.
3. Una duración de 60 minutos, si fuera configurada, reserva todos los slots consecutivos requeridos.

**Tareas técnicas:** caso de uso transaccional de reserva; integrar política de slots; definir transición SYSTEM; contrato y pruebas de carrera.

**DoD específica:** pruebas demuestran autoaprobación, revalidación bajo contención y ocupación exacta de slots.

### HU-021 — Solicitar cita especializada con retención

**Estado:** Propuesta · **Esfuerzo:** Muy alto · **Trazabilidad:** RF-12, RN-01, RN-03, RN-05, RN-08 · **Dependencias:** [[historias#HU-018--Consultar-disponibilidad-filtrada|HU-018]], [[historias#HU-020--Confirmar-cita-general-aprobada-automáticamente|HU-020]].

Como USER, quiero solicitar una cita especializada con sede, profesional y horario para que ADMIN la decida sin doble reserva.

**Criterios de aceptación:**
1. La solicitud válida nace en `REQUESTED`, conserva los datos relacionales de la selección y retiene todos los slots necesarios.
2. No se acepta especialidad/profesional/sede/horario que ya no sea elegible al confirmar.
3. La retención impide que otra solicitud o cita use la misma franja hasta la decisión administrativa.

**Tareas técnicas:** caso de uso transaccional; persistir cita/retención; transiciones explícitas; contrato y pruebas de conflictos.

**DoD específica:** evidencia demuestra estado `REQUESTED`, retención íntegra y rechazo seguro de doble solicitud.

### HU-022 — Consultar bandeja administrativa de decisiones

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-18 · **Dependencias:** [[historias#HU-021--Solicitar-cita-especializada-con-retención|HU-021]], [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como ADMIN, quiero ver citas especializadas `REQUESTED` con filtros para decidirlas de forma operativa.

**Criterios de aceptación:**
1. La bandeja muestra solo solicitudes `REQUESTED` y permite filtrar por sede, profesional, especialidad y fecha.
2. Un actor no ADMIN no puede consultar ni inferir el contenido de la bandeja.
3. Cada registro contiene la información mínima para decidir, sin exponer secretos o datos ajenos no necesarios.

**Tareas técnicas:** consulta/indexación; filtros REST; proyección administrativa; autorización; pruebas de filtros y acceso.

**DoD específica:** pruebas validan cada filtro y que la bandeja excluye estados distintos de `REQUESTED`.

### HU-023 — Aprobar o rechazar cita especializada

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-12, RN-03–04, RN-09, RN-11 · **Dependencias:** [[historias#HU-021--Solicitar-cita-especializada-con-retención|HU-021]], [[historias#HU-022--Consultar-bandeja-administrativa-de-decisiones|HU-022]].

Como ADMIN, quiero aprobar o rechazar una solicitud especializada para resolver su retención correctamente.

**Criterios de aceptación:**
1. Aprobar una solicitud `REQUESTED` la lleva a `APPROVED` y conserva los slots ya retenidos como reserva de la cita.
2. Rechazar exige un motivo, cambia a `REJECTED` y libera todos sus slots.
3. Decidir una solicitud ya resuelta o inexistente falla sin alterar citas ni reservas.

**Tareas técnicas:** definir máquina de estados; transacción decisión/liberación; validación de motivo; auditoría; pruebas de idempotencia/error.

**DoD específica:** hay pruebas de ambas decisiones, motivo obligatorio, liberación al rechazo y protección ante doble decisión.

### HU-024 — Consultar mis citas y su detalle

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-13 · **Dependencias:** [[historias#HU-020--Confirmar-cita-general-aprobada-automáticamente|HU-020]], [[historias#HU-021--Solicitar-cita-especializada-con-retención|HU-021]], [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como USER, quiero consultar y filtrar mis citas para conocer su estado y detalles relevantes.

**Criterios de aceptación:**
1. El listado pertenece exclusivamente al USER autenticado y permite filtrar por estado y fecha.
2. Cada detalle muestra sede, profesional, especialidad, fecha/hora, duración y estado.
3. Una cita rechazada muestra su motivo de rechazo; otros motivos se muestran solo cuando correspondan por estado/autorización.

**Tareas técnicas:** consulta/proyección por ownership; filtros e índices; endpoint de detalle; pruebas de privacidad y presentación de motivo.

**DoD específica:** pruebas verifican filtros, aislamiento de ownership y detalle completo para una cita rechazada.

### HU-025 — Cancelar una cita futura

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-14, RN-06, RN-09, RN-11 · **Dependencias:** [[historias#HU-020--Confirmar-cita-general-aprobada-automáticamente|HU-020]], [[historias#HU-023--Aprobar-o-rechazar-cita-especializada|HU-023]], [[historias#HU-024--Consultar-mis-citas-y-su-detalle|HU-024]].

Como USER, quiero cancelar mi cita futura no terminal para liberar su franja sin reactivarla directamente.

**Criterios de aceptación:**
1. Solo el USER dueño puede cancelar una cita futura en estado no terminal permitido por la máquina de estados.
2. La transición deja la cita en `CANCELLED`, libera sus slots y no permite reactivarla directamente.
3. Intentar cancelar una cita pasada, terminal, ajena o ya cancelada no modifica disponibilidad.

**Tareas técnicas:** definir transiciones permitidas; caso de uso transaccional; liberar slots; auditoría; pruebas negativas.

**DoD específica:** evidencia cubre cancelación válida, liberación, no reactivación y cada condición inválida relevante.

### HU-026 — Consultar agenda propia del profesional

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-16 · **Dependencias:** [[historias#HU-020--Confirmar-cita-general-aprobada-automáticamente|HU-020]], [[historias#HU-023--Aprobar-o-rechazar-cita-especializada|HU-023]], [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como PROFESSIONAL, quiero consultar mis citas `APPROVED` por día/semana y sede para atender mi agenda.

**Criterios de aceptación:**
1. La agenda permite filtros de día/semana y sede, y solo muestra citas `APPROVED` propias.
2. No aparecen citas de otros profesionales ni datos de usuarios que no correspondan a citas propias.
3. Los campos de usuario expuestos se limitan a lo necesario para la atención definida por el producto.

**Tareas técnicas:** consulta por ownership/período/sede; proyección mínima; contrato; pruebas de aislamiento de profesional.

**DoD específica:** pruebas prueban filtros, exclusión de otros profesionales y minimización de datos de usuario.

### HU-027 — Cerrar atención como completada o no asistió

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-17, RN-11 · **Dependencias:** [[historias#HU-026--Consultar-agenda-propia-del-profesional|HU-026]].

Como PROFESSIONAL, quiero marcar una cita aplicable como `COMPLETED` o `NO_SHOW` para cerrar su ciclo.

**Criterios de aceptación:**
1. Solo el profesional dueño puede cerrar una cita `APPROVED` pasada/aplicable conforme a la regla documentada.
2. Los únicos estados de cierre disponibles son `COMPLETED` y `NO_SHOW`.
3. La operación no permite cerrar una cita ajena, futura o en estado no elegible.

**Tareas técnicas:** precisar condición “pasada/aplicable”; transición de estado; autorización; auditoría; pruebas de límites.

**DoD específica:** la condición temporal está documentada y pruebas cubren ambos cierres y los rechazos por ownership/estado/fecha.

### HU-028 — Registrar auditoría inmutable de estados

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-19, RN-11–12, 3FN historial · **Dependencias:** [[historias#HU-020--Confirmar-cita-general-aprobada-automáticamente|HU-020]], [[historias#HU-023--Aprobar-o-rechazar-cita-especializada|HU-023]], [[historias#HU-025--Cancelar-una-cita-futura|HU-025]], [[historias#HU-027--Cerrar-atención-como-completada-o-no-asistió|HU-027]].

Como sistema, quiero registrar cada cambio de estado de cita para poder auditar actor, fuente, fecha y motivo sin CRUD normal.

**Criterios de aceptación:**
1. Cada transición registra cita, nuevo estado, actor si existe, fuente `SYSTEM`/`USER`/`ADMIN`, fecha/hora y motivo opcional.
2. El historial no tiene operaciones CRUD de modificación/eliminación expuestas como uso normal.
3. Los cambios automáticos, de USER, ADMIN y PROFESSIONAL usan la fuente/actor coherente definida en la especificación.

**Tareas técnicas:** modelar historial e índices; integrar puerto de auditoría en transiciones; decidir acceso de consulta; pruebas por fuente.

**DoD específica:** pruebas demuestran al menos una transición por fuente aplicable y la imposibilidad de mutar auditoría por endpoint normal.

### HU-029 — Consolidar contrato REST del ciclo de citas

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-11–14, RF-16–20 · **Dependencias:** [[historias#HU-020--Confirmar-cita-general-aprobada-automáticamente|HU-020]]–[[historias#HU-028--Registrar-auditoría-inmutable-de-estados|HU-028]].

Como equipo, quiero un contrato REST del ciclo de citas para que frontend y backend validen los mismos estados y errores.

**Criterios de aceptación:**
1. Reservar, solicitar, decidir, listar, cancelar, consultar agenda y cerrar atención documentan esquemas, autorización, códigos y errores.
2. El contrato enumera estados, duración/slots visibles y motivos requeridos sin filtrar detalles internos o tokens.
3. Hay casos de contrato para conflicto de disponibilidad, transición inválida y acceso no autorizado.

**Tareas técnicas:** completar especificación REST; enlazar máquina de estados y decisiones de concurrencia; preparar pruebas de integración/contrato.

**DoD específica:** una revisión puede mapear cada endpoint del ciclo a sus HU y reglas de negocio sin contradicciones.

## E05 — Reprogramación segura

### HU-030 — Solicitar reprogramación conservando la cita original

**Estado:** Propuesta · **Esfuerzo:** Muy alto · **Trazabilidad:** RF-15, RN-01, RN-10 · **Dependencias:** [[historias#HU-018--Consultar-disponibilidad-filtrada|HU-018]], [[historias#HU-023--Aprobar-o-rechazar-cita-especializada|HU-023]], [[historias#HU-024--Consultar-mis-citas-y-su-detalle|HU-024]].

Como USER, quiero solicitar una nueva franja para mi cita aprobada futura sin perder la cita original mientras ADMIN decide.

**Criterios de aceptación:**
1. Solo una cita `APPROVED` futura y propia puede iniciar solicitud; conserva profesional y especialidad.
2. La nueva fecha/hora debe estar disponible para ese profesional/especialidad y se retiene mientras el estado de reprogramación es `PENDING`.
3. La cita original y sus slots permanecen intactos hasta una decisión; cambiar de profesional no es reprogramación y se trata como nueva cita.

**Tareas técnicas:** modelar solicitud/estado y referencia a cita; transacción de retención; validar invariantes; contrato; pruebas de no pérdida.

**DoD específica:** pruebas demuestran retención nueva, conservación de franja original y rechazo de cita/fecha/profesional no elegibles.

### HU-031 — Consultar bandeja de reprogramaciones pendientes

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-18, RF-15 · **Dependencias:** [[historias#HU-030--Solicitar-reprogramación-conservando-la-cita-original|HU-030]], [[historias#HU-005--Aplicar-autorización-por-rol-y-ownership|HU-005]].

Como ADMIN, quiero consultar reprogramaciones `PENDING` con filtros para decidirlas.

**Criterios de aceptación:**
1. La bandeja muestra solo solicitudes `PENDING` y filtra por sede, profesional, especialidad y fecha.
2. Cada fila permite identificar cita original y franja solicitada sin exponer datos innecesarios.
3. USER y PROFESSIONAL no pueden acceder a esta bandeja administrativa.

**Tareas técnicas:** consulta/indexación y proyección; filtros REST; autorización; pruebas de estado/roles.

**DoD específica:** evidencia prueba filtros, exclusión de estados no pendientes y bloqueo de roles no ADMIN.

### HU-032 — Aprobar o rechazar reprogramación

**Estado:** Propuesta · **Esfuerzo:** Muy alto · **Trazabilidad:** RF-15, RN-01, RN-04, RN-09–11 · **Dependencias:** [[historias#HU-030--Solicitar-reprogramación-conservando-la-cita-original|HU-030]], [[historias#HU-031--Consultar-bandeja-de-reprogramaciones-pendientes|HU-031]].

Como ADMIN, quiero decidir una reprogramación pendiente para mover la cita solo cuando corresponda.

**Criterios de aceptación:**
1. Al aprobar, se liberan slots antiguos, se asignan los nuevos retenidos y se actualiza la cita preservando profesional/especialidad.
2. Al rechazar, se exige motivo, se liberan los slots provisionales y la cita original permanece sin cambio.
3. Una solicitud no `PENDING` no puede decidirse de nuevo ni generar liberaciones duplicadas.

**Tareas técnicas:** máquina de estados de reprogramación; transacción atómica de swap/liberación; auditoría de cita; pruebas de fallos y concurrencia.

**DoD específica:** pruebas verifican ambos desenlaces, el orden atómico de actualización y que jamás se pierden ambas franjas ante un error.

### HU-033 — Exponer trazabilidad de reprogramación y contrato

**Estado:** Propuesta · **Esfuerzo:** Medio · **Trazabilidad:** RF-15, RF-19, RF-20, 3FN reprogramación · **Dependencias:** [[historias#HU-030--Solicitar-reprogramación-conservando-la-cita-original|HU-030]]–[[historias#HU-032--Aprobar-o-rechazar-reprogramación|HU-032]].

Como USER y equipo integrador, quiero ver el resultado de mi reprogramación y un contrato inequívoco para seguir su trazabilidad.

**Criterios de aceptación:**
1. Mis citas/detalle muestran el resultado aplicable de reprogramación y el motivo de rechazo cuando exista.
2. El contrato documenta crear, listar/decidir ADMIN y errores de estado, disponibilidad y autorización.
3. La documentación enlaza la relación entre cita original, solicitud, retención y registros de auditoría.

**Tareas técnicas:** ampliar proyección USER; especificar endpoints/esquemas; enlazar ER, estados y pruebas de contrato.

**DoD específica:** se puede reconstruir desde documentación y datos sintéticos qué franja era original, cuál fue solicitada y cuál resultado se aplicó.

## E06 — Experiencia web y contrato

### HU-034 — Preparar base web de integración directa

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-20, restricciones frontend · **Dependencias:** [[historias#HU-006--Especificar-el-contrato-REST-de-identidad|HU-006]], [[historias#HU-019--Documentar-contrato-de-disponibilidad-y-agenda|HU-019]], [[historias#HU-029--Consolidar-contrato-REST-del-ciclo-de-citas|HU-029]].

Como equipo web, quiero una base TypeScript en React o Angular seleccionada por el diseño aprobado para consumir `citas-api` directamente.

**Criterios de aceptación:**
1. Se selecciona React o Angular conforme a la exportación/selección de Stitch + Google AI Studio.
2. La URL de backend es configurable por environment y no existe Express ni BFF.
3. El cliente maneja autenticación, errores REST y restricción de ruta por rol sin guardar secretos de servidor.

**Tareas técnicas:** registrar decisión de framework/diseño; configurar environment; capa de cliente REST; guardas de ruta; estrategia de pruebas/build.

**DoD específica:** build/typecheck aplicable y prueba de consumo del contrato de identidad verifican configuración sin secretos reales.

### HU-035 — Completar flujos web de USER

**Estado:** Propuesta · **Esfuerzo:** Muy alto · **Trazabilidad:** pantallas USER, RF-01–04, RF-10–15 · **Dependencias:** [[historias#HU-034--Preparar-base-web-de-integración-directa|HU-034]], [[historias#HU-033--Exponer-trazabilidad-de-reprogramación-y-contrato|HU-033]].

Como USER, quiero completar registro, acceso, perfil, búsqueda, citas y reprogramación desde la interfaz aprobada.

**Criterios de aceptación:**
1. Existen las pantallas obligatorias de registro, login, recuperación/cambio, dashboard, disponibilidad, solicitud, mis citas/detalle y reprogramación.
2. Cada pantalla usa los contratos aprobados y presenta validaciones/errores sin inventar estados o reglas.
3. Un USER solo navega y visualiza recursos propios; no recibe información de bandejas administrativas ni agendas ajenas.

**Tareas técnicas:** implementar vistas y estados de UI según diseño; conectar contratos; formularios/validación; pruebas de flujo y accesibilidad aplicable.

**DoD específica:** pruebas aplicables cubren al menos un flujo completo de registro/acceso, cita y reprogramación/rechazo usando datos sintéticos.

### HU-036 — Completar flujos web de PROFESSIONAL

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** pantallas PROFESSIONAL, RF-08, RF-16–17 · **Dependencias:** [[historias#HU-034--Preparar-base-web-de-integración-directa|HU-034]], [[historias#HU-027--Cerrar-atención-como-completada-o-no-asistió|HU-027]].

Como PROFESSIONAL, quiero gestionar bloques, consultar calendario/agenda y cerrar atenciones desde mi dashboard.

**Criterios de aceptación:**
1. Existen dashboard, gestión de bloques/calendario y agenda profesional por día/semana/sede.
2. La interfaz bloquea o explica acciones inválidas, sin sustituir la validación del servidor.
3. La UI no muestra citas ajenas ni datos de pacientes fuera de las citas propias.

**Tareas técnicas:** vistas de agenda/bloques; consumo de contratos; filtros y manejo de errores; pruebas por rol/ownership.

**DoD específica:** prueba de flujo demuestra crear bloque válido, consultar agenda propia y cerrar una cita elegible.

### HU-037 — Completar flujos web de ADMIN

**Estado:** Propuesta · **Esfuerzo:** Muy alto · **Trazabilidad:** pantallas ADMIN, RF-06–07, RF-12, RF-15, RF-18 · **Dependencias:** [[historias#HU-034--Preparar-base-web-de-integración-directa|HU-034]], [[historias#HU-032--Aprobar-o-rechazar-reprogramación|HU-032]].

Como ADMIN, quiero gestionar catálogos/profesionales y decidir solicitudes desde mi dashboard.

**Criterios de aceptación:**
1. Existen dashboard, CRUD de profesionales, especialidades, EPS/planes y bandejas de citas/reprogramaciones.
2. Rechazar cita o reprogramación exige motivo y la UI refleja el resultado del contrato.
3. La interfaz oculta acciones no administrativas y respeta activación/desactivación de catálogos referenciados.

**Tareas técnicas:** vistas CRUD/bandejas; formularios de motivo; consumo de filtros/decisiones; pruebas de acciones y autorización.

**DoD específica:** prueba de flujo demuestra administrar un catálogo y resolver una cita/reprogramación con validación de motivo.

### HU-038 — Verificar integración web-API y trazabilidad final

**Estado:** Propuesta · **Esfuerzo:** Alto · **Trazabilidad:** RF-20, pruebas S3+, seguridad y repositorios · **Dependencias:** [[historias#HU-035--Completar-flujos-web-de-USER|HU-035]]–[[historias#HU-037--Completar-flujos-web-de-ADMIN|HU-037]].

Como equipo, quiero verificar los flujos clave entre `citas-web` y `citas-api` para entregar una experiencia coherente y trazable.

**Criterios de aceptación:**
1. Flujos clave por rol validan contratos REST reales: identidad, disponibilidad, reserva/decisión, cancelación y reprogramación.
2. Backend tiene pruebas de dominio/aplicación/integración relevantes y frontend tiene build/typecheck/pruebas aplicables al framework elegido.
3. La documentación enlaza HU, contrato, decisión, evidencia y commits trazables por sesión S2–S6 sin reescribir historial.

**Tareas técnicas:** definir matriz de escenarios cross-repo; ejecutar validaciones pertinentes; actualizar enlaces de evidencia; revisar CORS/environment y datos sintéticos.

**DoD específica:** matriz de escenarios muestra resultado verificable por rol y ningún escenario requiere secretos, datos reales ni un BFF.
