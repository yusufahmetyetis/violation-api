package com.yusuf.jwt_demo.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Yönetim işlemleri. **Rol:** ADMIN")
public class AdminController {

    // Entity'yi doğrudan döndürmüyoruz, yoksa password hash'i de JSON'a çıkar!
    @Schema(description = "Kullanıcı özeti (parola hash'i bilinçli olarak yok)")
    public record UserDto(
            @Schema(example = "user") String username,
            @Schema(description = "false ise giriş yapamaz", example = "true") boolean enabled,
            @Schema(description = "Sadece USER rolünde dolu", example = "34A0261", nullable = true) String plate,
            @Schema(example = "[\"USER\"]") List<String> roles) { }

    private final AppUserRepository repo;

    public AdminController(AppUserRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/users")
    @Operation(summary = "Kullanıcıları listele", description = "**Rol:** ADMIN")
    public List<UserDto> users() {
        return repo.findAll().stream()
                .map(u -> new UserDto(u.getUsername(), u.isEnabled(), u.getPlate(),
                        repo.findRoleNames(u.getId())))
                .toList();
    }
}
