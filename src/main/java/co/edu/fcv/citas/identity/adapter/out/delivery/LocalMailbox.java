package co.edu.fcv.citas.identity.adapter.out.delivery;

import co.edu.fcv.citas.identity.application.port.out.PasswordResetPorts.RecoveryDeliveryPort;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Canal de entrega de desarrollo (RF-03: SMTP no obligatorio). Guarda en memoria los últimos mensajes de
 * recuperación; solo existe con {@code app.recovery.local-mailbox=true} y se lee por un endpoint ADMIN.
 * Nunca escribe el token en logs.
 */
@Component
@ConditionalOnProperty(name = "app.recovery.local-mailbox", havingValue = "true")
public class LocalMailbox implements RecoveryDeliveryPort {
    private static final int CAPACITY = 50;
    private final Deque<Message> messages = new ArrayDeque<>();

    @Override
    public synchronized void deliver(String email, String token, Instant expiresAt) {
        messages.addFirst(new Message(email, token, expiresAt, Instant.now()));
        while (messages.size() > CAPACITY) messages.removeLast();
    }

    /** Del más reciente al más antiguo. */
    public synchronized List<Message> recent() {
        return List.copyOf(messages);
    }

    public record Message(String email, String token, Instant expiresAt, Instant createdAt) { }
}
