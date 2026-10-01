package co.edu.fcv.citas.offer.adapter.out.identity;

import co.edu.fcv.citas.identity.application.port.in.CreateStaffAccountUseCase;
import co.edu.fcv.citas.identity.domain.Document;
import co.edu.fcv.citas.identity.domain.EmailAddress;
import co.edu.fcv.citas.identity.domain.PersonalData;
import co.edu.fcv.citas.identity.domain.Role;
import co.edu.fcv.citas.offer.application.port.out.ProfessionalAccountPort;
import org.springframework.stereotype.Component;

/** Delega la creación de la identidad PROFESSIONAL al módulo de identidad (mismas reglas que el registro). */
@Component
class ProfessionalAccountAdapter implements ProfessionalAccountPort {
    private final CreateStaffAccountUseCase staffAccounts;

    ProfessionalAccountAdapter(CreateStaffAccountUseCase staffAccounts) {
        this.staffAccounts = staffAccounts;
    }

    @Override
    public long createAccount(String firstName, String lastName, String documentType, String documentNumber, String email, String phone,
                              String initialPassword) {
        PersonalData data = new PersonalData(firstName, lastName, new Document(documentType, documentNumber), EmailAddress.of(email), phone);
        return staffAccounts.create(data, initialPassword, Role.PROFESSIONAL);
    }
}
