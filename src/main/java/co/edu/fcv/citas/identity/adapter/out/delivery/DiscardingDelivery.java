package co.edu.fcv.citas.identity.adapter.out.delivery;

import co.edu.fcv.citas.identity.application.port.out.PasswordResetPorts.RecoveryDeliveryPort;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Sin buzón local ni SMTP configurado: el token no se entrega y solo se registra el evento, sin datos sensibles. */
@Component
@ConditionalOnProperty(name = "app.recovery.local-mailbox", havingValue = "false", matchIfMissing = true)
class DiscardingDelivery implements RecoveryDeliveryPort {
    private static final Logger log = LoggerFactory.getLogger(DiscardingDelivery.class);

    @Override
    public void deliver(String email, String token, Instant expiresAt) {
        log.info("Recuperación de contraseña solicitada; no hay canal de entrega configurado");
    }
}
