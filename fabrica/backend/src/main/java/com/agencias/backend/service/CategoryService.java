package com.agencias.backend.service;

import com.agencias.backend.model.Category;
import com.agencias.backend.repository.CategoryRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

public class CategoryService {
    private final CategoryRepository repo;

    public CategoryService(EntityManagerFactory emf) {
        this.repo = new CategoryRepository(emf);
    }

    public Category create(String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        Category c = new Category();
        c.setName(name.trim());
        c.setParentId(parentId);
        return repo.save(c);
    }

    public Category update(Long id, String name, Long parentId) {
        Category c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        if (name != null && !name.isBlank()) {
            c.setName(name.trim());
        }
        c.setParentId(parentId);
        return repo.save(c);
    }

    public List<Category> listAll() {
        return repo.findAll();
    }

    public Category getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void delete(Long id) {
        repo.delete(id);
    }

    public Category updateImage(Long id, byte[] imageData, String imageType) {
        Category c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        c.setImageData(imageData);
        c.setImageType(imageType);
        return repo.save(c);
    }
}
