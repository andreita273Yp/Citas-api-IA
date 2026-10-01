package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase.ProfessionalOption;
import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase.SpecialtyOption;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.FreeSlotPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.FreeSlotRow;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.OfferPort;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts.SpecialtyRules;
import co.edu.fcv.citas.scheduling.domain.AvailabilityCalculator.FreeSlot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Modelo de lectura de la oferta reservable: solo combina profesional, especialidad y sede vigentes (RN-07/RN-08). */
@Component
class OfferAvailabilityReadModel implements OfferPort, FreeSlotPort {
    /** Condición común de oferta válida para un profesional {@code p}, especialidad {@code ps} y sede {@code pl}. */
    private static final String OFFERED = "p.active=true and ps.active=true and s.active=true and pl.active=true and l.active=true";
    private final JdbcTemplate db;

    OfferAvailabilityReadModel(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public Optional<SpecialtyRules> findActiveSpecialty(long specialtyId) {
        return db.query("select id,appointment_duration_minutes,is_general from specialties where id=? and active=true",
                (rs, n) -> new SpecialtyRules(rs.getLong(1), rs.getInt(2), rs.getBoolean(3)), specialtyId).stream().findFirst();
    }

    @Override
    public boolean offers(long professionalId, long specialtyId, long locationId) {
        return db.queryForObject("select count(*) from professionals p join professional_specialties ps on ps.professional_id=p.id "
                + "join specialties s on s.id=ps.specialty_id join professional_locations pl on pl.professional_id=p.id join locations l on l.id=pl.location_id "
                + "where p.id=? and ps.specialty_id=? and pl.location_id=? and " + OFFERED, Integer.class, professionalId, specialtyId, locationId) > 0;
    }

    @Override
    public List<SpecialtyOption> activeSpecialties(Boolean general) {
        return db.query("select id,code,name,appointment_duration_minutes,is_general,requires_admin_approval from specialties "
                        + "where active=true and (? is null or is_general=?) order by name",
                (rs, n) -> new SpecialtyOption(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getBoolean(5), rs.getBoolean(6)),
                general, general);
    }

    @Override
    public List<ProfessionalOption> offeredProfessionals(long specialtyId, long locationId) {
        return db.query("select p.id,concat(u.first_name,' ',u.last_name),p.professional_code from professionals p join users u on u.id=p.user_id "
                        + "join professional_specialties ps on ps.professional_id=p.id join specialties s on s.id=ps.specialty_id "
                        + "join professional_locations pl on pl.professional_id=p.id join locations l on l.id=pl.location_id "
                        + "where ps.specialty_id=? and pl.location_id=? and " + OFFERED + " order by 2",
                (rs, n) -> new ProfessionalOption(rs.getLong(1), rs.getString(2), rs.getString(3)), specialtyId, locationId);
    }

    @Override
    public List<FreeSlotRow> freeSlots(long specialtyId, LocalDate date, Long locationId, Long professionalId) {
        return db.query("select p.id,b.location_id,sl.start_at,sl.end_at,concat(u.first_name,' ',u.last_name),l.code "
                        + "from professional_slots sl join availability_blocks b on b.id=sl.availability_block_id "
                        + "join professionals p on p.id=b.professional_id join users u on u.id=p.user_id "
                        + "join professional_specialties ps on ps.professional_id=p.id and ps.specialty_id=? join specialties s on s.id=ps.specialty_id "
                        + "join professional_locations pl on pl.professional_id=p.id and pl.location_id=b.location_id join locations l on l.id=b.location_id "
                        + "where b.active=true and sl.appointment_id is null and b.available_date=? and " + OFFERED
                        + " and (? is null or b.location_id=?) and (? is null or p.id=?) order by p.id,b.location_id,sl.start_at",
                (rs, n) -> new FreeSlotRow(new FreeSlot(rs.getLong(1), rs.getLong(2), rs.getTimestamp(3).toLocalDateTime(), rs.getTimestamp(4).toLocalDateTime()),
                        rs.getString(5), rs.getString(6)),
                specialtyId, date, locationId, locationId, professionalId, professionalId);
    }
}
