package com.agencias.backend.service;

import com.agencias.backend.model.Role;
import com.agencias.backend.repository.RoleRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;

public class RoleService {
    private static final List<String> DEFAULT_ROLE_NAMES = Arrays.asList("ADMIN", "REGISTERED", "ENTERPRISE");

    private final RoleRepository roleRepo;

    public RoleService(EntityManagerFactory emf) {
        this.roleRepo = new RoleRepository(emf);
    }

    /**
     * Asegura que existan todos los roles por defecto (ADMIN, REGISTERED, ENTERPRISE).
     */
    public void ensureDefaultRoles() {
        for (String name : DEFAULT_ROLE_NAMES) {
            roleRepo.findByName(name).orElseGet(() -> {
                Role r = new Role();
                r.setName(name);
                return roleRepo.save(r);
            });
        }
    }

    public List<Role> listRoles() {
        ensureDefaultRoles();
        return roleRepo.findAll();
    }

    public Role getById(Long id) {
        return roleRepo.findById(id).orElse(null);
    }
}
