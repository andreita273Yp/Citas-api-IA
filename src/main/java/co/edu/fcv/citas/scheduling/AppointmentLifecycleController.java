package co.edu.fcv.citas.scheduling;

import co.edu.fcv.citas.scheduling.adapter.out.persistence.AppointmentStatusHistory;
import co.edu.fcv.citas.shared.web.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for a USER's own appointments and their rescheduling lifecycle (modelo de referencia V6).
 * Una reprogramación PENDING retiene su nueva franja asignando esos slots a la misma cita; la franja
 * original se conserva hasta que ADMIN decide (RN-10).
 */
@RestController
@RequestMapping("/api/v1")
public class AppointmentLifecycleController {
  private static final String VIEW = "select a.id,a.location_id,l.name,concat(pu.first_name,' ',pu.last_name),s.name,s.appointment_duration_minutes,"
      + "a.scheduled_start_at,a.scheduled_end_at,st.code,a.reason,"
      + "case when st.code='REJECTED' then (select h.reason from appointment_status_history h where h.appointment_id=a.id and h.status_id=a.status_id order by h.changed_at desc,h.id desc limit 1) end "
      + "from appointments a join locations l on l.id=a.location_id join professionals p on p.id=a.professional_id join users pu on pu.id=p.user_id "
      + "join specialties s on s.id=a.specialty_id join appointment_statuses st on st.id=a.status_id ";

  private final JdbcTemplate db;
  private final AppointmentStatusHistory history;

  public AppointmentLifecycleController(JdbcTemplate db, AppointmentStatusHistory history) {
    this.db = db;
    this.history = history;
  }

  @GetMapping("/appointments")
  public List<AppointmentView> list(@RequestParam(required=false) String status,
      @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to) {
    if (from != null && to != null && from.isAfter(to)) throw bad("El rango de fechas es inválido");
    return db.query(VIEW + "where a.patient_user_id=? and (? is null or st.code=?) and (? is null or a.scheduled_start_at>=?) and (? is null or a.scheduled_start_at<?) order by a.scheduled_start_at desc",
        (rs,n)->new AppointmentView(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6),rs.getTimestamp(7).toLocalDateTime(),rs.getTimestamp(8).toLocalDateTime(),rs.getString(9),rs.getString(10),rs.getString(11)),
        actor(),status,status,from,start(from),to,endExclusive(to));
  }

  @GetMapping("/appointments/{id}")
  public AppointmentView detail(@PathVariable long id) {
    List<AppointmentView> result = db.query(VIEW + "where a.id=? and a.patient_user_id=?",
        (rs,n)->new AppointmentView(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6),rs.getTimestamp(7).toLocalDateTime(),rs.getTimestamp(8).toLocalDateTime(),rs.getString(9),rs.getString(10),rs.getString(11)),id,actor());
    if (result.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,"Cita no existe");
    return result.getFirst();
  }

  @PostMapping("/appointments/{id}/cancel")
  @Transactional
  public AppointmentView cancel(@PathVariable long id) {
    Appointment row=own(id);
    if (!("APPROVED".equals(row.status)||"REQUESTED".equals(row.status))) throw conflict("La cita ya está en un estado terminal");
    if (!row.start.isAfter(LocalDateTime.now())) throw conflict("Solo puede cancelar una cita futura");
    setStatus(id,"CANCELLED");
    // Libera la franja propia y cualquier franja retenida por una reprogramación pendiente (RN-09).
    db.update("update professional_slots set appointment_id=null where appointment_id=?",id);
    db.update("update reschedule_requests set status_id=(select id from reschedule_request_statuses where code='CANCELLED'),decided_at=now() "
        + "where appointment_id=? and status_id=(select id from reschedule_request_statuses where code='PENDING')",id);
    history.record(id,"CANCELLED",actor(),"USER",null);
    return detail(id);
  }

  @PostMapping("/appointments/{id}/reschedule-requests")
  @Transactional
  public RescheduleView request(@PathVariable long id,@RequestBody RescheduleInput input) {
    Appointment row=own(id);
    if (!"APPROVED".equals(row.status)) throw conflict("Solo se puede reprogramar una cita aprobada");
    if (!row.start.isAfter(LocalDateTime.now())) throw conflict("Solo puede reprogramar una cita futura");
    LocalDateTime at=parse(input.startAt());
    if (!at.isAfter(LocalDateTime.now())||at.getMinute()%30!=0||at.getSecond()!=0||at.getNano()!=0) throw bad("La nueva franja debe ser futura y estar en intervalos de 30 minutos");
    long location=input.locationId()==null||input.locationId().isBlank()?row.locationId:parseId(input.locationId());
    if (db.queryForObject("select count(*) from professional_locations where professional_id=? and location_id=? and active=true",Integer.class,row.professionalId,location)==0) throw conflict("El profesional no atiende en la sede seleccionada");
    if (db.queryForObject("select count(*) from reschedule_requests rr join reschedule_request_statuses rs on rs.id=rr.status_id where rr.appointment_id=? and rs.code='PENDING'",Integer.class,id)>0) throw conflict("Ya existe una solicitud de reprogramación pendiente");
    LocalDateTime until=at.plusMinutes(row.duration);
    List<Long> slots=db.query("select ps.id from professional_slots ps join availability_blocks ab on ab.id=ps.availability_block_id "
        + "where ab.professional_id=? and ab.location_id=? and ab.active=true and ps.start_at>=? and ps.end_at<=? and ps.appointment_id is null order by ps.start_at for update",
        (rs,n)->rs.getLong(1),row.professionalId,location,at,until);
    if (slots.size()!=row.duration/30) throw conflict("La nueva franja ya no está disponible");
    for (long slot:slots) if (db.update("update professional_slots set appointment_id=? where id=? and appointment_id is null",id,slot)==0) throw conflict("La nueva franja ya no está disponible");
    db.update("insert into reschedule_requests(appointment_id,requested_by_user_id,requested_location_id,status_id,previous_start_at,previous_end_at,requested_start_at,requested_end_at) "
        + "values(?,?,?,(select id from reschedule_request_statuses where code='PENDING'),?,?,?,?)",id,actor(),location,row.start,row.end,at,until);
    long requestId=db.queryForObject("select last_insert_id()",Long.class);
    return new RescheduleView(Long.toString(requestId),Long.toString(id),"PENDING",Long.toString(location),at,until,null);
  }

  @PostMapping("/admin/reschedule-requests/{id}/decision")
  @Transactional
  public RescheduleView decide(@PathVariable long id,@RequestBody Decision input) {
    admin();
    if(!"APPROVE".equals(input.decision())&&!"REJECT".equals(input.decision())) throw bad("Decisión inválida");
    if("REJECT".equals(input.decision())&&blank(input.reason())==null) throw bad("El rechazo exige un motivo");
    List<Request> requests=db.query("select rr.id,rr.appointment_id,rs.code,rr.requested_location_id,rr.requested_start_at,rr.requested_end_at from reschedule_requests rr "
        + "join reschedule_request_statuses rs on rs.id=rr.status_id where rr.id=? for update",
        (rs,n)->new Request(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getLong(4),rs.getTimestamp(5).toLocalDateTime(),rs.getTimestamp(6).toLocalDateTime()),id);
    if(requests.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,"Solicitud no existe");
    Request request=requests.getFirst();
    if(!"PENDING".equals(request.status())) throw conflict("La solicitud ya fue decidida");
    Appointment appointment=any(request.appointmentId());
    int held=db.queryForObject("select count(*) from professional_slots where appointment_id=? and start_at>=? and end_at<=?",Integer.class,appointment.id,request.startsAt(),request.endsAt());
    if(held!=appointment.duration/30) throw conflict("La retención de franjas es inconsistente");
    String next="APPROVE".equals(input.decision())?"APPROVED":"REJECTED";
    if("APPROVED".equals(next)) {
      // Libera la franja anterior y la cita pasa a ocupar la franja retenida.
      db.update("update professional_slots set appointment_id=null where appointment_id=? and not (start_at>=? and end_at<=?)",appointment.id,request.startsAt(),request.endsAt());
      db.update("update appointments set location_id=?,scheduled_start_at=?,scheduled_end_at=? where id=?",request.locationId(),request.startsAt(),request.endsAt(),appointment.id);
      history.record(appointment.id,"APPROVED",actor(),"ADMIN","Reprogramación aprobada");
    } else {
      // Libera solo la franja retenida; la cita original se mantiene (RF-15).
      db.update("update professional_slots set appointment_id=null where appointment_id=? and start_at>=? and end_at<=?",appointment.id,request.startsAt(),request.endsAt());
    }
    db.update("update reschedule_requests set status_id=(select id from reschedule_request_statuses where code=?),decision_reason=?,decided_by_user_id=?,decided_at=now() where id=?",
        next,blank(input.reason()),actor(),id);
    return new RescheduleView(Long.toString(id),Long.toString(appointment.id),next,Long.toString(request.locationId()),request.startsAt(),request.endsAt(),blank(input.reason()));
  }

  private void setStatus(long id,String code){db.update("update appointments set status_id=(select id from appointment_statuses where code=?) where id=?",code,id);}
  private Appointment own(long id){return find(APPOINTMENT+"where a.id=? and a.patient_user_id=? for update",id,actor());}
  private Appointment any(long id){return find(APPOINTMENT+"where a.id=? for update",id);}
  private static final String APPOINTMENT="select a.id,a.professional_id,a.location_id,a.scheduled_start_at,a.scheduled_end_at,st.code,s.appointment_duration_minutes "
      + "from appointments a join specialties s on s.id=a.specialty_id join appointment_statuses st on st.id=a.status_id ";
  private Appointment find(String sql,Object... args){List<Appointment> r=db.query(sql,(rs,n)->new Appointment(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getTimestamp(4).toLocalDateTime(),rs.getTimestamp(5).toLocalDateTime(),rs.getString(6),rs.getInt(7)),args);if(r.isEmpty())throw new ApiException(HttpStatus.NOT_FOUND,"Cita no existe");return r.getFirst();}
  private long actor(){Authentication a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!a.isAuthenticated())throw new ApiException(HttpStatus.UNAUTHORIZED,"Autenticación requerida");try{return Long.parseLong(a.getName());}catch(NumberFormatException e){throw new ApiException(HttpStatus.UNAUTHORIZED,"Usuario inválido");}}
  private void admin(){if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_ADMIN")))throw new ApiException(HttpStatus.FORBIDDEN,"Rol insuficiente");}
  private static long parseId(String v){try{return Long.parseLong(v.trim());}catch(NumberFormatException e){throw bad("Sede inválida");}}
  private static LocalDateTime parse(String v){try{return LocalDateTime.parse(v);}catch(Exception e){throw bad("Fecha y hora inválida; use YYYY-MM-DDTHH:mm");}}
  private static LocalDateTime start(LocalDate d){return d==null?null:d.atStartOfDay();}
  private static LocalDateTime endExclusive(LocalDate d){return d==null?null:d.plusDays(1).atStartOfDay();}
  private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
  private static ApiException bad(String s){return new ApiException(HttpStatus.BAD_REQUEST,s);}
  private static ApiException conflict(String s){return new ApiException(HttpStatus.CONFLICT,s);}
  public record AppointmentView(String id,String locationId,String location,String professional,String specialty,int durationMinutes,LocalDateTime startsAt,LocalDateTime endsAt,String status,String reason,String decisionReason){}
  public record RescheduleInput(String locationId,String startAt,String reason){}
  public record Decision(String decision,String reason){}
  public record RescheduleView(String id,String appointmentId,String status,String locationId,LocalDateTime startsAt,LocalDateTime endsAt,String decisionReason){}
  private record Request(long id,long appointmentId,String status,long locationId,LocalDateTime startsAt,LocalDateTime endsAt){}
  private record Appointment(long id,long professionalId,long locationId,LocalDateTime start,LocalDateTime end,String status,int duration){}
}
