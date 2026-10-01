package co.edu.fcv.citas.offer.application.port.in;

import co.edu.fcv.citas.offer.domain.ProfessionalView;
import co.edu.fcv.citas.offer.domain.SpecialtyAssignments;
import java.util.List;

/** HU-015/016/017 · El ADMIN crea profesionales, gestiona sus especialidades y sedes, y los activa o desactiva. */
public interface ManageProfessionalsUseCase {
    List<ProfessionalView> list();

    ProfessionalView get(long id);

    ProfessionalView create(NewProfessional command);

    ProfessionalView setActive(long id, boolean active);

    ProfessionalView assignSpecialties(long id, SpecialtyAssignments assignments);

    ProfessionalView assignLocations(long id, List<Long> locationIds);

    record NewProfessional(String firstName, String lastName, String documentType, String documentNumber, String email,
                           String phone, String initialPassword, String professionalCode, String licenseNumber) { }
}
