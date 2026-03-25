package com.agencias.backend.service;

import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.EnterpriseProfile;
import com.agencias.backend.model.Role;
import com.agencias.backend.repository.AppUserRepository;
import com.agencias.backend.repository.EnterpriseProfileRepository;
import com.agencias.backend.repository.RoleRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EnterpriseProfileService {
    private final EnterpriseProfileRepository profileRepo;
    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;

    public EnterpriseProfileService(EntityManagerFactory emf) {
        this.profileRepo = new EnterpriseProfileRepository(emf);
        this.userRepo = new AppUserRepository(emf);
        this.roleRepo = new RoleRepository(emf);
    }

    public Optional<EnterpriseProfile> getByUserId(Long userId) {
        return profileRepo.findByUserId(userId);
    }

    public List<EnterpriseProfile> listAll() {
        return profileRepo.findAll();
    }

    /**
     * Admin: asigna usuario como empresarial y configura descuento.
     * Crea o actualiza EnterpriseProfile y asigna rol ENTERPRISE.
     */
    public EnterpriseProfile assignEnterprise(Long userId, BigDecimal discountPercent, Long adminUserId) {
        AppUser admin = userRepo.findById(adminUserId).orElse(null);
        if (admin == null || admin.getRoles().stream().noneMatch(r -> "ADMIN".equals(r.getName()))) {
            throw new SecurityException("Solo un administrador puede asignar perfil empresarial");
        }
        AppUser target = userRepo.findById(userId).orElse(null);
        if (target == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Role enterpriseRole = roleRepo.findByName("ENTERPRISE")
            .orElseGet(() -> {
                Role r = new Role();
                r.setName("ENTERPRISE");
                return roleRepo.save(r);
            });

        if (target.getRoles().stream().noneMatch(r -> "ENTERPRISE".equals(r.getName()))) {
            target.getRoles().add(enterpriseRole);
            userRepo.save(target);
        }

        EnterpriseProfile profile = profileRepo.findByUserId(userId).orElseGet(() -> {
            EnterpriseProfile p = new EnterpriseProfile();
            p.setUserId(userId);
            p.setApiKey(UUID.randomUUID().toString().replace("-", ""));
            return p;
        });
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) >= 0 && discountPercent.compareTo(BigDecimal.valueOf(100)) < 0) {
            profile.setDiscountPercent(discountPercent);
        }
        return profileRepo.save(profile);
    }

    /**
     * Quitar perfil empresarial (admin): quita rol ENTERPRISE. El perfil se mantiene por historial pero el usuario deja de ser enterprise.
     */
    public void unassignEnterprise(Long userId, Long adminUserId) {
        AppUser admin = userRepo.findById(adminUserId).orElse(null);
        if (admin == null || admin.getRoles().stream().noneMatch(r -> "ADMIN".equals(r.getName()))) {
            throw new SecurityException("Solo un administrador puede quitar perfil empresarial");
        }
        AppUser target = userRepo.findById(userId).orElse(null);
        if (target == null) return;
        target.getRoles().removeIf(r -> "ENTERPRISE".equals(r.getName()));
        userRepo.save(target);
    }

    /**
     * Usuario empresarial: actualiza su perfil (dirección default, tarjeta default, horario de entrega).
     */
    public EnterpriseProfile updateProfile(Long userId, String defaultAddressText, String defaultCardToken,
                                           String defaultCardLast4, String deliveryWindow) {
        EnterpriseProfile profile = profileRepo.findByUserId(userId)
            .orElseThrow(() -> new IllegalArgumentException("No tienes perfil empresarial. Contacta al administrador."));
        AppUser user = userRepo.findById(userId).orElse(null);
        if (user == null || user.getRoles().stream().noneMatch(r -> "ENTERPRISE".equals(r.getName()))) {
            throw new SecurityException("Solo usuarios empresariales pueden actualizar este perfil");
        }
        if (defaultAddressText != null) profile.setDefaultAddressText(defaultAddressText.trim().isEmpty() ? null : defaultAddressText.trim());
        if (defaultCardToken != null) profile.setDefaultCardToken(defaultCardToken.trim().isEmpty() ? null : defaultCardToken.trim());
        if (defaultCardLast4 != null) profile.setDefaultCardLast4(defaultCardLast4.trim().isEmpty() ? null : defaultCardLast4.trim());
        if (deliveryWindow != null) profile.setDeliveryWindow(deliveryWindow.trim().isEmpty() ? null : deliveryWindow.trim());
        return profileRepo.save(profile);
    }
}
