package ee.itjobs.controller;

import ee.itjobs.dto.admin.JobUpdateRequest;
import ee.itjobs.dto.admin.UserListDto;
import ee.itjobs.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get paginated user list with search")
    public ResponseEntity<Page<UserListDto>> getUsers(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.getUsers(search, pageable));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserListDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUser(id));
    }

    @PutMapping("/users/{id}/toggle-active")
    @Operation(summary = "Toggle user active status")
    public ResponseEntity<Void> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        adminService.toggleUserActive(id, active);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/toggle-admin")
    @Operation(summary = "Toggle user admin role")
    public ResponseEntity<Void> toggleAdmin(
            @PathVariable Long id,
            @RequestParam boolean admin) {
        adminService.toggleUserAdmin(id, admin);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/jobs/{id}")
    @Operation(summary = "Update job details")
    public ResponseEntity<Void> updateJob(
            @PathVariable Long id,
            @RequestBody JobUpdateRequest request) {
        adminService.updateJob(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/jobs/{id}")
    @Operation(summary = "Delete a job")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        adminService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
