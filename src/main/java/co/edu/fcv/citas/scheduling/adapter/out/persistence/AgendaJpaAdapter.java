package co.edu.fcv.citas.scheduling.adapter.out.persistence;

import co.edu.fcv.citas.scheduling.application.port.in.ManageBlocksUseCase.BlockView;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts.BlockPort;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts.Owner;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts.ProfessionalPort;
import co.edu.fcv.citas.scheduling.domain.BlockSchedule;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Bloques y slots con Spring Data JPA; las vistas con sede y conteos se leen con SQL. */
@Component
class AgendaJpaAdapter implements BlockPort, ProfessionalPort {
    private static final String VIEW = "select b.id,b.location_id,l.code,b.available_date,b.start_time,b.end_time,"
            + "(select count(*) from professional_slots s where s.availability_block_id=b.id),"
            + "(select count(*) from professional_slots s where s.availability_block_id=b.id and s.appointment_id is not null) "
            + "from availability_blocks b join locations l on l.id=b.location_id where b.active=true and b.professional_id=? ";
    private final AvailabilityBlockJpaRepository blocks;
    private final ProfessionalSlotJpaRepository slots;
    private final JdbcTemplate db;

    AgendaJpaAdapter(AvailabilityBlockJpaRepository blocks, ProfessionalSlotJpaRepository slots, JdbcTemplate db) {
        this.blocks = blocks;
        this.slots = slots;
        this.db = db;
    }

    @Override
    public Optional<Owner> findByUserId(long userId) {
        return db.query("select id,active from professionals where user_id=?", (rs, n) -> new Owner(rs.getLong(1), rs.getBoolean(2),
                new HashSet<>(db.queryForList("select location_id from professional_locations where professional_id=? and active=true",
                        Long.class, rs.getLong(1)))), userId).stream().findFirst();
    }

    @Override
    public void lock(long professionalId) {
        db.queryForList("select id from professionals where id=? for update", Long.class, professionalId);
    }

    @Override
    public List<BlockView> findOwn(long professionalId, LocalDate from, LocalDate to, Long locationId) {
        return db.query(VIEW + "and (? is null or b.available_date>=?) and (? is null or b.available_date<=?) and (? is null or b.location_id=?) "
                        + "order by b.available_date,b.start_time",
                (rs, n) -> view(rs), professionalId, from, from, to, to, locationId, locationId);
    }

    @Override
    public Optional<BlockView> lockOwn(long professionalId, long blockId) {
        if (blocks.lockOwn(professionalId, blockId).isEmpty()) return Optional.empty();
        return db.query(VIEW + "and b.id=?", (rs, n) -> view(rs), professionalId, blockId).stream().findFirst();
    }

    @Override
    public boolean overlaps(long professionalId, BlockSchedule s, Long excludeBlockId) {
        return blocks.overlaps(professionalId, s.date(), s.start(), s.end(), excludeBlockId);
    }

    @Override
    public long create(long professionalId, long locationId, BlockSchedule s) {
        AvailabilityBlockEntity e = new AvailabilityBlockEntity();
        e.professionalId = professionalId;
        e.locationId = locationId;
        apply(e, s);
        long id = blocks.saveAndFlush(e).id;
        publishSlots(id, s);
        return id;
    }

    @Override
    public int lockCommittedSlots(long blockId) {
        return (int) slots.findByBlockIdOrderByStartAt(blockId).stream().filter(s -> s.appointmentId != null).count();
    }

    @Override
    public void reschedule(long blockId, long locationId, BlockSchedule s) {
        slots.deleteByBlock(blockId);
        AvailabilityBlockEntity e = blocks.findById(blockId).orElseThrow();
        e.locationId = locationId;
        apply(e, s);
        blocks.saveAndFlush(e);
        publishSlots(blockId, s);
    }

    @Override
    public void delete(long blockId) {
        slots.deleteByBlock(blockId);
        blocks.deleteById(blockId);
        blocks.flush();
    }

    private void publishSlots(long blockId, BlockSchedule s) {
        slots.saveAllAndFlush(s.slots().stream().map(t -> new ProfessionalSlotEntity(blockId, t.start(), t.end())).toList());
    }

    private static void apply(AvailabilityBlockEntity e, BlockSchedule s) {
        e.date = s.date();
        e.startTime = s.start();
        e.endTime = s.end();
    }

    private static BlockView view(java.sql.ResultSet rs) throws java.sql.SQLException {
        BlockSchedule schedule = new BlockSchedule(rs.getDate(4).toLocalDate(), rs.getTime(5).toLocalTime(), rs.getTime(6).toLocalTime());
        return new BlockView(rs.getLong(1), rs.getLong(2), rs.getString(3), schedule, rs.getInt(7), rs.getInt(8));
    }
}
