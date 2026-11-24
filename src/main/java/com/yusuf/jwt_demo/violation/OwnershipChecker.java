package com.yusuf.jwt_demo.violation;

import com.yusuf.jwt_demo.user.AppUser;
import com.yusuf.jwt_demo.user.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("ownership")          // @PreAuthorize içinde @ownership adıyla çağrılıyor
public class OwnershipChecker {

    private final AppUserRepository userRepo;
    private final ViolationRepository violationRepo;

    public OwnershipChecker(AppUserRepository userRepo, ViolationRepository violationRepo) {
        this.userRepo = userRepo;
        this.violationRepo = violationRepo;
    }

    public String plateOf(Authentication auth) {
        // auth.getName() = token'daki "sub" (kullanıcı adı)
        return userRepo.findByUsername(auth.getName()).map(AppUser::getPlate).orElse(null);
    }

    public boolean ownsViolation(Long violationId, Authentication auth) {
        String plate = plateOf(auth);
        return plate != null && violationRepo.findById(violationId)
                .map(v -> plate.equals(v.getPlate()))
                .orElse(false);   // kayıt yoksa da false → 403 (var/yok bilgisini sızdırmayalım)
    }
}
