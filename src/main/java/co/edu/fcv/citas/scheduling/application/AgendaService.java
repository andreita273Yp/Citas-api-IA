package co.edu.fcv.citas.scheduling.application;

import co.edu.fcv.citas.scheduling.application.port.in.ManageBlocksUseCase;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts.BlockPort;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts.Owner;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts.ProfessionalPort;
import co.edu.fcv.citas.scheduling.domain.BlockSchedule;
import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-018/019/020 · Agenda propia del PROFESSIONAL: sin pasado ni solapamientos, solo en sedes asignadas y
 * sin tocar bloques con citas comprometidas (RN-06, RN-07).
 */
public class AgendaService implements ManageBlocksUseCase {
    private final ProfessionalPort professionals;
    private final BlockPort blocks;
    private final TransactionPort tx;
    private final Clock clock;

    public AgendaService(ProfessionalPort professionals, BlockPort blocks, TransactionPort tx, Clock clock) {
        this.professionals = professionals;
        this.blocks = blocks;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public long professionalIdOf(long userId) {
        return owner(userId).professionalId();
    }

    @Override
    public List<BlockView> list(long userId, LocalDate from, LocalDate to, Long locationId) {
        if (from != null && to != null && from.isAfter(to)) throw new SchedulingException.InvalidData("El rango de fechas es inválido");
        return blocks.findOwn(owner(userId).professionalId(), from, to, locationId);
    }

    @Override
    public BlockView create(long userId, long locationId, BlockSchedule schedule) {
        schedule.requireFuture(now());
        return tx.inTransaction(() -> {
            Owner owner = lockedPublisher(userId, locationId);
            if (blocks.overlaps(owner.professionalId(), schedule, null)) throw overlap();
            long id = blocks.create(owner.professionalId(), locationId, schedule);
            return blocks.lockOwn(owner.professionalId(), id).orElseThrow();
        });
    }

    @Override
    public BlockView update(long userId, long blockId, long locationId, BlockSchedule schedule) {
        schedule.requireFuture(now());
        return tx.inTransaction(() -> {
            Owner owner = lockedPublisher(userId, locationId);
            changeableBlock(owner, blockId);
            if (blocks.overlaps(owner.professionalId(), schedule, blockId)) throw overlap();
            blocks.reschedule(blockId, locationId, schedule);
            return blocks.lockOwn(owner.professionalId(), blockId).orElseThrow();
        });
    }

    @Override
    public void delete(long userId, long blockId) {
        tx.inTransaction(() -> {
            Owner owner = owner(userId);
            professionals.lock(owner.professionalId());
            changeableBlock(owner, blockId);
            blocks.delete(blockId);
            return null;
        });
    }

    /** Un bloque propio, que aún no empieza y sin slots ocupados o retenidos (HU-019 CA-03). */
    private void changeableBlock(Owner owner, long blockId) {
        BlockView block = blocks.lockOwn(owner.professionalId(), blockId)
                .orElseThrow(() -> new SchedulingException.NotFound("Bloque no existe"));
        if (!block.schedule().startsAt().isAfter(now())) throw new SchedulingException.Conflict("El bloque ya inició o pasó");
        if (blocks.lockCommittedSlots(blockId) > 0) throw new SchedulingException.Conflict("El bloque tiene citas comprometidas");
    }

    /** RN-07 y HU-017 CA-03: el profesional debe estar activo y asignado a la sede. */
    private Owner lockedPublisher(long userId, long locationId) {
        Owner owner = owner(userId);
        professionals.lock(owner.professionalId());
        if (!owner.active()) throw new SchedulingException.Conflict("El profesional está inactivo");
        if (!owner.locationIds().contains(locationId)) throw new SchedulingException.Conflict("El profesional no está asignado a esa sede");
        return owner;
    }

    private Owner owner(long userId) {
        return professionals.findByUserId(userId).orElseThrow(() -> new SchedulingException.Forbidden("El usuario no es un profesional registrado"));
    }

    private static SchedulingException overlap() {
        return new SchedulingException.Conflict("El bloque se solapa con otro bloque del profesional");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
