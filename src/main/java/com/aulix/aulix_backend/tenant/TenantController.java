package com.aulix.aulix_backend.tenant;

import com.aulix.aulix_backend.shared.dto.ApiResponse;
import com.aulix.aulix_backend.tenant.dto.CreateTenantRequest;
import com.aulix.aulix_backend.tenant.model.Tenant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantProvisioningService tenantProvisioningService;

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Tenant>> create(
            @Valid @RequestBody CreateTenantRequest request) {

        Tenant tenant = tenantProvisioningService.createTenant(
                request.getSlug(),
                request.getName(),
                request.getBrandColor()
        );

        return ResponseEntity.ok(ApiResponse.ok("Tenant creado exitosamente", tenant));
    }


}
