package co.edu.fcv.citas.identity;
import java.util.*; import org.springframework.data.jpa.repository.*; import jakarta.persistence.LockModeType;
public interface RefreshSessionRepository extends JpaRepository<RefreshSession,String>{ @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<RefreshSession> findByJtiHash(String hash); long deleteByUserId(String userId); }
