package co.edu.fcv.citas.scheduling.application.port.in;

import co.edu.fcv.citas.scheduling.domain.BlockSchedule;
import java.time.LocalDate;
import java.util.List;

/** HU-018/019/020 · El PROFESSIONAL publica, edita, elimina y consulta sus propios bloques. */
public interface ManageBlocksUseCase {
    /** Profesional asociado al usuario autenticado; {@code Forbidden} si el usuario no es profesional. */
    long professionalIdOf(long userId);

    List<BlockView> list(long userId, LocalDate from, LocalDate to, Long locationId);

    BlockView create(long userId, long locationId, BlockSchedule schedule);

    BlockView update(long userId, long blockId, long locationId, BlockSchedule schedule);

    void delete(long userId, long blockId);

    record BlockView(long id, long locationId, String locationCode, BlockSchedule schedule, int slots, int bookedSlots) { }
}
