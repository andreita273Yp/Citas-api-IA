package co.edu.fcv.citas.insurance.adapter.in.web;

import co.edu.fcv.citas.insurance.application.port.in.InsuranceUseCases.ManageAffiliation;
import co.edu.fcv.citas.insurance.application.port.in.InsuranceUseCases.ManageCatalog;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.AffiliationView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.EpsView;
import co.edu.fcv.citas.insurance.domain.InsuranceViews.PlanView;
import co.edu.fcv.citas.shared.security.CurrentUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-011 a HU-013 · Catálogo público de selección (`/catalogs/eps*`, sin sesión para el registro), afiliación
 * propia (`/users/me/affiliation`) y CRUD lógico ADMIN (`/admin/eps*`; no existe DELETE, se desactiva).
 */
@RestController
@RequestMapping("/api/v1")
class InsuranceController {
    private final ManageCatalog catalog;
    private final ManageAffiliation affiliations;

    InsuranceController(ManageCatalog catalog, ManageAffiliation affiliations) {
        this.catalog = catalog;
        this.affiliations = affiliations;
    }

    // ---------------------------------------------------------------- catálogo de selección

    @GetMapping("/catalogs/eps")
    List<EpsView> selectableEps() {
        return catalog.selectableEps();
    }

    @GetMapping("/catalogs/eps-plans")
    List<PlanView> selectablePlans(@RequestParam long epsId) {
        return catalog.selectablePlans(epsId);
    }

    // ---------------------------------------------------------------- afiliación propia

    @GetMapping("/users/me/affiliation")
    ResponseEntity<AffiliationView> current() {
        return affiliations.current(CurrentUser.id()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/users/me/affiliation")
    AffiliationView affiliate(@RequestBody AffiliationRequest body) {
        return affiliations.affiliate(CurrentUser.id(), body.planId(), body.membershipNumber());
    }

    @DeleteMapping("/users/me/affiliation")
    ResponseEntity<Void> end() {
        affiliations.end(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- administración

    @GetMapping("/admin/eps")
    List<EpsView> listEps() {
        return catalog.listEps();
    }

    @PostMapping("/admin/eps")
    ResponseEntity<EpsView> createEps(@RequestBody EpsRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.createEps(body.code(), body.name()));
    }

    @PatchMapping("/admin/eps/{id}")
    EpsView updateEps(@PathVariable long id, @RequestBody EpsChange body) {
        return catalog.updateEps(id, body.name(), body.active());
    }

    @GetMapping("/admin/eps-plans")
    List<PlanView> listPlans(@RequestParam(required = false) Long epsId) {
        return catalog.listPlans(epsId);
    }

    @PostMapping("/admin/eps-plans")
    ResponseEntity<PlanView> createPlan(@RequestBody PlanRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.createPlan(body.epsId(), body.regimeId(), body.code(), body.name()));
    }

    @PatchMapping("/admin/eps-plans/{id}")
    PlanView updatePlan(@PathVariable long id, @RequestBody PlanChange body) {
        return catalog.updatePlan(id, body.name(), body.regimeId(), body.active());
    }

    record AffiliationRequest(Long planId, String membershipNumber) { }

    record EpsRequest(String code, String name) { }

    record EpsChange(String name, Boolean active) { }

    record PlanRequest(Long epsId, Long regimeId, String code, String name) { }

    record PlanChange(String name, Long regimeId, Boolean active) { }
}
