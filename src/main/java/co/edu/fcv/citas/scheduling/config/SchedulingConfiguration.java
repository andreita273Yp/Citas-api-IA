package co.edu.fcv.citas.scheduling.config;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.scheduling.application.AgendaService;
import co.edu.fcv.citas.scheduling.application.AvailabilityService;
import co.edu.fcv.citas.scheduling.application.BookingService;
import co.edu.fcv.citas.scheduling.application.DecisionService;
import co.edu.fcv.citas.scheduling.application.port.in.BookAppointmentUseCase;
import co.edu.fcv.citas.scheduling.application.port.in.DecideSpecializedRequestUseCase;
import co.edu.fcv.citas.scheduling.application.port.in.ManageBlocksUseCase;
import co.edu.fcv.citas.scheduling.application.port.in.SearchAvailabilityUseCase;
import co.edu.fcv.citas.scheduling.application.port.out.AgendaPorts;
import co.edu.fcv.citas.scheduling.application.port.out.BookingPorts;
import co.edu.fcv.citas.shared.application.TransactionPort;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla los casos de uso de agenda y citas; la aplicación no conoce Spring. */
@Configuration
class SchedulingConfiguration {
    /** Reloj de negocio en America/Bogota: fuente única de "ahora" para las reglas de pasado/futuro. */
    @Bean
    Clock businessClock() {
        return Clock.system(ZoneId.of(CitasApiApplication.BUSINESS_ZONE));
    }

    @Bean
    ManageBlocksUseCase manageBlocksUseCase(AgendaPorts.ProfessionalPort professionals, AgendaPorts.BlockPort blocks, TransactionPort tx, Clock clock) {
        return new AgendaService(professionals, blocks, tx, clock);
    }

    @Bean
    SearchAvailabilityUseCase searchAvailabilityUseCase(BookingPorts.OfferPort offer, BookingPorts.FreeSlotPort freeSlots, Clock clock) {
        return new AvailabilityService(offer, freeSlots, clock);
    }

    @Bean
    BookAppointmentUseCase bookAppointmentUseCase(BookingPorts.OfferPort offer, BookingPorts.SlotPort slots, BookingPorts.AppointmentPort appointments,
                                                  BookingPorts.StatusHistoryPort history, TransactionPort tx, Clock clock) {
        return new BookingService(offer, slots, appointments, history, tx, clock);
    }

    @Bean
    co.edu.fcv.citas.scheduling.application.AppointmentLifecycleService appointmentLifecycleService(
            co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.AppointmentQueryPort queries,
            co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.AppointmentWritePort appointments,
            co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.ReschedulePort reschedules, BookingPorts.SlotPort slots,
            co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.HeldSlotPort held, BookingPorts.OfferPort offer,
            BookingPorts.StatusHistoryPort history, TransactionPort tx, Clock clock) {
        return new co.edu.fcv.citas.scheduling.application.AppointmentLifecycleService(queries, appointments, reschedules, slots, held, offer, history, tx, clock);
    }

    @Bean
    co.edu.fcv.citas.scheduling.application.OperationsService operationsService(AgendaPorts.ProfessionalPort professionals,
            co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.AgendaQueryPort agenda,
            co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.AppointmentWritePort appointments,
            co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.ReschedulePort reschedules,
            co.edu.fcv.citas.scheduling.application.port.out.LifecyclePorts.HeldSlotPort held,
            co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.InboxQueryPort inbox,
            co.edu.fcv.citas.scheduling.application.port.out.OperationsPorts.HistoryQueryPort historyQueries,
            BookingPorts.StatusHistoryPort history, TransactionPort tx, Clock clock) {
        return new co.edu.fcv.citas.scheduling.application.OperationsService(professionals, agenda, appointments, reschedules, held, inbox,
                historyQueries, history, tx, clock);
    }

    @Bean
    DecideSpecializedRequestUseCase decideSpecializedRequestUseCase(BookingPorts.AppointmentPort appointments, BookingPorts.SlotPort slots,
                                                                    BookingPorts.StatusHistoryPort history, TransactionPort tx, Clock clock) {
        return new DecisionService(appointments, slots, history, tx, clock);
    }
}
