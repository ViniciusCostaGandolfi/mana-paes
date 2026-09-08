package vgandolfi.dev.mana_paes.infrastructure.web.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.mana_paes.application.dto.request.UserRequest;
import vgandolfi.dev.mana_paes.application.dto.request.UserStatusRequest;
import vgandolfi.dev.mana_paes.application.dto.response.UserResponse;
import vgandolfi.dev.mana_paes.application.service.UserService;
import vgandolfi.dev.mana_paes.infrastructure.security.AuthenticatedUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<Page<UserResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(userService.list(principal.tenantId(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> getById(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(principal.tenantId(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(principal.tenantId(), principal.role(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable UUID id,
                                               @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.update(principal.tenantId(), principal.role(), id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> setStatus(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(userService.setActive(principal.tenantId(), id, request.active()));
    }
}