package com.agencias.backend.service;

import com.agencias.backend.model.Brand;
import com.agencias.backend.repository.BrandRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

public class BrandService {
    private final BrandRepository repo;

    public BrandService(EntityManagerFactory emf) {
        this.repo = new BrandRepository(emf);
    }

    public Brand create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        Brand b = new Brand();
        b.setName(name.trim());
        return repo.save(b);
    }

    public Brand update(Long id, String name) {
        Brand b = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));
        if (name != null && !name.isBlank()) {
            b.setName(name.trim());
        }
        return repo.save(b);
    }

    public List<Brand> listAll() {
        return repo.findAll();
    }

    public Brand getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void delete(Long id) {
        repo.delete(id);
    }

    public Brand updateImage(Long id, byte[] imageData, String imageType) {
        Brand b = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Marca no encontrada"));
        b.setImageData(imageData);
        b.setImageType(imageType);
        return repo.save(b);
    }
}
