package co.edu.fcv.citas.identity.adapter.in.web;

import co.edu.fcv.citas.identity.adapter.out.delivery.LocalMailbox;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-008 CA-03 · Buzón local de desarrollo, solo ADMIN (`/api/v1/admin/**`) y solo si el buzón está habilitado. */
@RestController
@ConditionalOnProperty(name = "app.recovery.local-mailbox", havingValue = "true")
class LocalMailboxController {
    private final LocalMailbox mailbox;

    LocalMailboxController(LocalMailbox mailbox) {
        this.mailbox = mailbox;
    }

    @GetMapping("/api/v1/admin/local-mailbox/password-recovery")
    List<LocalMailbox.Message> passwordRecovery() {
        return mailbox.recent();
    }
}
