package com.agencias.backend.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.Role;
import com.agencias.backend.repository.AppUserRepository;
import com.agencias.backend.repository.RoleRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserService {
    private static final int BCRYPT_COST = 12;

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
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }
        if (userRepo.findByEmail(email.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        String hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
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
        return BCrypt.verifyer().verify(plainPassword.toCharArray(), hash).verified;
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
