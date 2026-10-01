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

/** Professional operations and the protected administrative inbox/audit read model (modelo de referencia V6). */
@RestController
@RequestMapping("/api/v1")
public class OperationsController {
  private final JdbcTemplate db;
  private final AppointmentStatusHistory history;

  public OperationsController(JdbcTemplate db, AppointmentStatusHistory history) {
    this.db = db;
    this.history = history;
  }

  @GetMapping("/professional/appointments")
  public List<ProfessionalAppointment> professionalAppointments(@RequestParam(required=false) LocalDate from,
      @RequestParam(required=false) LocalDate to, @RequestParam(required=false) Long locationId) {
    long professional=currentProfessional(); range(from,to);
    return db.query("select a.id,a.location_id,l.name,concat(u.first_name,' ',u.last_name),s.name,a.scheduled_start_at,a.scheduled_end_at,a.reason "
        + "from appointments a join locations l on l.id=a.location_id join users u on u.id=a.patient_user_id join specialties s on s.id=a.specialty_id "
        + "join appointment_statuses st on st.id=a.status_id "
        + "where a.professional_id=? and st.code='APPROVED' and (? is null or a.location_id=?) and (? is null or a.scheduled_start_at>=?) and (? is null or a.scheduled_start_at<?) order by a.scheduled_start_at",
        (rs,n)->new ProfessionalAppointment(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getTimestamp(6).toLocalDateTime(),rs.getTimestamp(7).toLocalDateTime(),rs.getString(8)),
        professional,locationId,locationId,from,start(from),to,end(to));
  }

  @PostMapping("/professional/appointments/{id}/closure")
  @Transactional
  public void close(@PathVariable long id,@RequestBody Closure input) {
    long professional=currentProfessional();
    if (!"COMPLETED".equals(input.status()) && !"NO_SHOW".equals(input.status())) throw bad("El cierre debe ser COMPLETED o NO_SHOW");
    List<Appointment> appointments=db.query("select a.id,a.scheduled_end_at,st.code from appointments a join appointment_statuses st on st.id=a.status_id where a.id=? and a.professional_id=? for update",
        (rs,n)->new Appointment(rs.getLong(1),rs.getTimestamp(2).toLocalDateTime(),rs.getString(3)),id,professional);
    if(appointments.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,"Cita no existe");
    Appointment appointment=appointments.getFirst();
    if(!"APPROVED".equals(appointment.status)) throw conflict("Solo se puede cerrar una cita aprobada");
    if(appointment.endsAt.isAfter(LocalDateTime.now())) throw conflict("La cita aún no ha terminado");
    db.update("update appointments set status_id=(select id from appointment_statuses where code=?) where id=?",input.status(),id);
    history.record(id,input.status(),actor(),"USER",blank(input.reason()));
  }

  @GetMapping("/admin/inbox")
  public List<InboxItem> inbox(@RequestParam(required=false) Long locationId,@RequestParam(required=false) Long professionalId,
      @RequestParam(required=false) Long specialtyId,@RequestParam(required=false) LocalDate date) {
    admin();
    String requested="select 'SPECIALIZED_REQUEST' kind,a.id request_id,a.id appointment_id,a.location_id,concat(u.first_name,' ',u.last_name) patient_name,concat(pu.first_name,' ',pu.last_name) professional_name,s.name specialty_name,a.scheduled_start_at requested_at,a.scheduled_end_at requested_end "
        + "from appointments a join appointment_statuses st on st.id=a.status_id join users u on u.id=a.patient_user_id join professionals p on p.id=a.professional_id join users pu on pu.id=p.user_id join specialties s on s.id=a.specialty_id "
        + "where st.code='REQUESTED' and (? is null or a.location_id=?) and (? is null or a.professional_id=?) and (? is null or a.specialty_id=?) and (? is null or date(a.scheduled_start_at)=?)";
    String reschedules="select 'RESCHEDULE' kind,r.id request_id,a.id appointment_id,r.requested_location_id,concat(u.first_name,' ',u.last_name) patient_name,concat(pu.first_name,' ',pu.last_name) professional_name,s.name specialty_name,r.requested_start_at requested_at,r.requested_end_at requested_end "
        + "from reschedule_requests r join reschedule_request_statuses rs on rs.id=r.status_id join appointments a on a.id=r.appointment_id join users u on u.id=a.patient_user_id join professionals p on p.id=a.professional_id join users pu on pu.id=p.user_id join specialties s on s.id=a.specialty_id "
        + "where rs.code='PENDING' and (? is null or r.requested_location_id=?) and (? is null or a.professional_id=?) and (? is null or a.specialty_id=?) and (? is null or date(r.requested_start_at)=?)";
    return db.query(requested+" union all "+reschedules+" order by requested_at",(rs,n)->new InboxItem(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getTimestamp(8).toLocalDateTime(),rs.getTimestamp(9).toLocalDateTime()),
        locationId,locationId,professionalId,professionalId,specialtyId,specialtyId,date,date,locationId,locationId,professionalId,professionalId,specialtyId,specialtyId,date,date);
  }

  @GetMapping("/appointments/{id}/history")
  public List<AppointmentStatusHistory.Entry> history(@PathVariable long id) {
    ensureHistoryAccess(id);
    return history.of(id);
  }

  private void ensureHistoryAccess(long id) {
    if(has("ADMIN")) return;
    Integer count=has("PROFESSIONAL")
        ? db.queryForObject("select count(*) from appointments a join professionals p on p.id=a.professional_id where a.id=? and p.user_id=?",Integer.class,id,actor())
        : db.queryForObject("select count(*) from appointments where id=? and patient_user_id=?",Integer.class,id,actor());
    if(count==0) throw new ApiException(HttpStatus.NOT_FOUND,"Cita no existe");
  }
  /** El JWT identifica al usuario; la agenda pertenece a su registro en professionals. */
  private long currentProfessional(){
    if(!has("PROFESSIONAL"))throw new ApiException(HttpStatus.FORBIDDEN,"Rol insuficiente");
    List<Long> ids=db.query("select id from professionals where user_id=? and active=true",(rs,n)->rs.getLong(1),actor());
    if(ids.isEmpty())throw new ApiException(HttpStatus.FORBIDDEN,"Profesional inactivo o no registrado");
    return ids.getFirst();
  }
  private long actor(){Authentication a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!a.isAuthenticated())throw new ApiException(HttpStatus.UNAUTHORIZED,"Autenticación requerida");try{return Long.parseLong(a.getName());}catch(NumberFormatException e){throw new ApiException(HttpStatus.UNAUTHORIZED,"Usuario inválido");}}
  private boolean has(String role){return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+role));}
  private void admin(){if(!has("ADMIN"))throw new ApiException(HttpStatus.FORBIDDEN,"Rol insuficiente");}
  private void range(LocalDate from,LocalDate to){if(from!=null&&to!=null&&from.isAfter(to))throw bad("El rango de fechas es inválido");}
  private static LocalDateTime start(LocalDate date){return date==null?null:date.atStartOfDay();}
  private static LocalDateTime end(LocalDate date){return date==null?null:date.plusDays(1).atStartOfDay();}
  private static String blank(String value){return value==null||value.isBlank()?null:value.trim();}
  private static ApiException bad(String message){return new ApiException(HttpStatus.BAD_REQUEST,message);}
  private static ApiException conflict(String message){return new ApiException(HttpStatus.CONFLICT,message);}
  public record ProfessionalAppointment(String id,String locationId,String location,String patient,String specialty,LocalDateTime startsAt,LocalDateTime endsAt,String reason){}
  public record Closure(String status,String reason){}
  public record InboxItem(String kind,String requestId,String appointmentId,String locationId,String patient,String professional,String specialty,LocalDateTime startsAt,LocalDateTime endsAt){}
  private record Appointment(long id,LocalDateTime endsAt,String status){}
}
