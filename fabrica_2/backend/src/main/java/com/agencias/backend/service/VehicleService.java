package com.agencias.backend.service;

import com.agencias.backend.model.Vehicle;
import com.agencias.backend.repository.VehicleRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

public class VehicleService {
    private final VehicleRepository repo;

    public VehicleService(EntityManagerFactory emf) {
        this.repo = new VehicleRepository(emf);
    }

    public Vehicle create(String universalCode, String make, String line, Integer year) {
        if (universalCode == null || universalCode.isBlank()) {
            throw new IllegalArgumentException("El código universal es obligatorio");
        }
        Vehicle v = new Vehicle();
        v.setUniversalVehicleCode(universalCode.trim());
        v.setMake(make != null ? make.trim() : null);
        v.setLine(line != null ? line.trim() : null);
        v.setYearNumber(year);
        return repo.save(v);
    }

    public Vehicle update(Long id, String universalCode, String make, String line, Integer year) {
        Vehicle v = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehículo no encontrado"));
        if (universalCode != null && !universalCode.isBlank()) {
            v.setUniversalVehicleCode(universalCode.trim());
        }
        if (make != null) v.setMake(make.trim());
        if (line != null) v.setLine(line.trim());
        if (year != null) v.setYearNumber(year);
        return repo.save(v);
    }

    public List<Vehicle> listAll() {
        return repo.findAll();
    }

    public Vehicle getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void delete(Long id) {
        repo.delete(id);
    }
}
