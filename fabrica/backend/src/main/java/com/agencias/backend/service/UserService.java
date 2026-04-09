package com.agencias.backend.service;

import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.Role;
import com.agencias.backend.repository.AppUserRepository;
import com.agencias.backend.repository.RoleRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserService {

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;

    public UserService(EntityManagerFactory emf) {
        this.userRepo = new AppUserRepository(emf);
        this.roleRepo = new RoleRepository(emf);
    }

    /**
     * Crea un usuario. Si es el primero del sistema, se le asigna rol ADMIN.
     * En caso contrario, rol REGISTERED.
     */
    public AppUser createUser(String email, String password, String fullName, String phone) {
        UserRegistrationValidator.validateEmailAndPassword(email, password);
        if (userRepo.findByEmail(email.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        String hash = PasswordEncoding.hash(password);
        AppUser user = new AppUser();
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(hash);
        user.setFullName(fullName != null ? fullName.trim() : null);
        user.setPhone(phone != null ? phone.trim() : null);
        user.setStatus("ACTIVE");

        boolean isFirstUser = userRepo.count() == 0;
        String roleName = isFirstUser ? "ADMIN" : "REGISTERED";
        Role roleToAssign = ensureRole(roleName);

        user.getRoles().add(roleToAssign);
        return userRepo.save(user);
    }

    public List<AppUser> listUsers() {
        return userRepo.findAll();
    }

    public AppUser getById(Long id) {
        return userRepo.findById(id).orElse(null);
    }

    /**
     * Usuario para comentarios desde portal de distribuidora (API key válida).
     * Reutiliza APP_USER por email o crea uno con contraseña aleatoria y rol REGISTERED.
     */
    public AppUser resolveOrCreatePortalUser(String rawEmail, String rawFullName) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail es obligatorio");
        }
        return userRepo.findByEmailIgnoreCase(rawEmail).orElseGet(() -> {
            String email = rawEmail.trim().toLowerCase();
            String randomSecret = UUID.randomUUID().toString() + "Aa1!extra";
            AppUser user = new AppUser();
            user.setEmail(email);
            user.setPasswordHash(PasswordEncoding.hash(randomSecret));
            user.setFullName(rawFullName != null && !rawFullName.isBlank() ? rawFullName.trim() : null);
            user.setStatus("ACTIVE");
            Role role = ensureRole("REGISTERED");
            user.getRoles().add(role);
            return userRepo.save(user);
        });
    }

    /**
     * Login: busca usuario por email y verifica contraseña.
     * @return el usuario si las credenciales son correctas, null en caso contrario.
     */
    public AppUser login(String email, String password) {
        if (email == null || email.isBlank() || password == null) {
            return null;
        }
        return userRepo.findByEmail(email.trim().toLowerCase())
            .filter(u -> verifyPassword(password, u.getPasswordHash()))
            .orElse(null);
    }

    /**
     * Asigna los roles de un usuario. Solo un admin puede hacerlo.
     * @param adminUserId ID del usuario que realiza la acción (debe ser ADMIN).
     */
    public AppUser assignRoles(Long userId, List<Long> roleIds, Long adminUserId) {
        AppUser admin = userRepo.findById(adminUserId).orElse(null);
        if (admin == null || admin.getRoles().stream().noneMatch(r -> "ADMIN".equals(r.getName()))) {
            throw new SecurityException("Solo un administrador puede asignar roles");
        }

        AppUser user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        List<Role> newRoles = new ArrayList<>();
        for (Long roleId : roleIds) {
            roleRepo.findById(roleId).ifPresent(newRoles::add);
        }
        user.setRoles(newRoles);
        return userRepo.save(user);
    }

    public boolean verifyPassword(String plainPassword, String hash) {
        return PasswordEncoding.verify(plainPassword, hash);
    }

    private Role ensureRole(String name) {
        return roleRepo.findByName(name)
            .orElseGet(() -> {
                Role r = new Role();
                r.setName(name);
                return roleRepo.save(r);
            });
    }
}
