package co.edu.fcv.citas.identity;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User,String>{ Optional<User> findByEmail(String email); boolean existsByEmail(String email); boolean existsByDocumentTypeAndDocumentNumber(String type,String number); }
