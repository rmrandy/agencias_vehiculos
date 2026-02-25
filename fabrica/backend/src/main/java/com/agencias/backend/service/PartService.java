package com.agencias.backend.service;

import com.agencias.backend.model.ImportExportLog;
import com.agencias.backend.model.InventoryLog;
import com.agencias.backend.model.Part;
import com.agencias.backend.repository.ImportExportLogRepository;
import com.agencias.backend.repository.InventoryLogRepository;
import com.agencias.backend.repository.PartRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PartService {
    private final PartRepository repo;
    private final MailService mailService;
    private final InventoryLogRepository inventoryLogRepo;
    private final ImportExportLogRepository importExportLogRepo;

    public PartService(EntityManagerFactory emf) {
        this(emf, null);
    }

    public PartService(EntityManagerFactory emf, MailService mailService) {
        this.repo = new PartRepository(emf);
        this.mailService = mailService;
        this.inventoryLogRepo = new InventoryLogRepository(emf);
        this.importExportLogRepo = new ImportExportLogRepository(emf);
    }

    private void checkLowStockAndNotify(Part p) {
        if (mailService == null) return;
        int stock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
        int reserved = p.getReservedQuantity() != null ? p.getReservedQuantity() : 0;
        int threshold = p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 5;
        if (stock - reserved <= threshold) {
            mailService.sendLowStockAlert(p);
        }
    }

    public Part create(Long categoryId, Long brandId, String partNumber, String title, 
                      String description, BigDecimal weightLb, BigDecimal price, 
                      Integer stockQuantity, Integer lowStockThreshold) {
        if (partNumber == null || partNumber.isBlank()) {
            throw new IllegalArgumentException("El número de parte es obligatorio");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio debe ser mayor o igual a cero");
        }
        if (repo.findByPartNumber(partNumber.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un repuesto con ese número de parte");
        }

        Part p = new Part();
        p.setCategoryId(categoryId);
        p.setBrandId(brandId);
        p.setPartNumber(partNumber.trim());
        p.setTitle(title.trim());
        p.setDescription(description != null ? description.trim() : null);
        p.setWeightLb(weightLb);
        p.setPrice(price);
        p.setStockQuantity(stockQuantity != null ? stockQuantity : 0);
        p.setLowStockThreshold(lowStockThreshold != null ? lowStockThreshold : 5);
        return repo.save(p);
    }

    public Part update(Long id, Long categoryId, Long brandId, String title, 
                      String description, BigDecimal weightLb, BigDecimal price, Integer active) {
        Part p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado"));
        if (categoryId != null) p.setCategoryId(categoryId);
        if (brandId != null) p.setBrandId(brandId);
        if (title != null && !title.isBlank()) p.setTitle(title.trim());
        if (description != null) p.setDescription(description.trim());
        if (weightLb != null) p.setWeightLb(weightLb);
        if (price != null) p.setPrice(price);
        if (active != null) p.setActive(active);
        return repo.save(p);
    }

    public Part updateImage(Long id, byte[] imageData, String imageType) {
        Part p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado"));
        p.setImageData(imageData);
        p.setImageType(imageType);
        return repo.save(p);
    }

    public List<Part> listAll() {
        return repo.findAll();
    }

    public List<Part> listByCategory(Long categoryId) {
        return repo.findByCategory(categoryId);
    }

    public List<Part> listByBrand(Long brandId) {
        return repo.findByBrand(brandId);
    }

    public Part getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public Part getByPartNumber(String partNumber) {
        return repo.findByPartNumber(partNumber).orElse(null);
    }

    public void delete(Long id) {
        repo.delete(id);
    }

    /**
     * Actualizar inventario de un repuesto
     */
    public Part updateInventory(Long id, Integer stockQuantity, Integer lowStockThreshold) {
        Part p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado"));
        if (stockQuantity != null && stockQuantity >= 0) {
            p.setStockQuantity(stockQuantity);
        }
        if (lowStockThreshold != null && lowStockThreshold >= 0) {
            p.setLowStockThreshold(lowStockThreshold);
        }
        p = repo.save(p);
        checkLowStockAndNotify(p);
        return p;
    }

    /**
     * Reservar inventario para un pedido
     * @return true si se pudo reservar, false si no hay suficiente stock
     */
    public boolean reserveStock(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        
        Part p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado"));
        
        int available = p.getStockQuantity() - p.getReservedQuantity();
        if (available < quantity) {
            return false; // No hay suficiente stock
        }
        
        p.setReservedQuantity(p.getReservedQuantity() + quantity);
        repo.save(p);
        return true;
    }

    /**
     * Confirmar venta y reducir stock
     */
    public void confirmSale(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        
        Part p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado"));
        
        // Reducir stock y cantidad reservada
        p.setStockQuantity(Math.max(0, p.getStockQuantity() - quantity));
        p.setReservedQuantity(Math.max(0, p.getReservedQuantity() - quantity));
        repo.save(p);
        checkLowStockAndNotify(p);
    }

    /**
     * Liberar inventario reservado (si se cancela un pedido)
     */
    public void releaseStock(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        
        Part p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado"));
        p.setReservedQuantity(Math.max(0, p.getReservedQuantity() - quantity));
        repo.save(p);
    }

    /**
     * Verificar si hay stock disponible
     */
    public boolean checkAvailability(Long id, Integer quantity) {
        Part p = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado"));
        int available = p.getStockQuantity() - p.getReservedQuantity();
        return available >= quantity;
    }

    /**
     * Agregar inventario a un repuesto y registrar el alta en el log.
     */
    public Part addInventory(Long partId, Long userId, int quantityAdded) {
        if (quantityAdded <= 0) {
            throw new IllegalArgumentException("La cantidad a agregar debe ser mayor a cero");
        }
        Part p = repo.findById(partId).orElseThrow(() -> new IllegalArgumentException("Repuesto no encontrado"));
        int previous = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
        int newQty = previous + quantityAdded;
        p.setStockQuantity(newQty);
        p = repo.save(p);

        InventoryLog log = new InventoryLog();
        log.setPartId(partId);
        log.setUserId(userId);
        log.setQuantityAdded(quantityAdded);
        log.setPreviousQuantity(previous);
        log.setNewQuantity(newQty);
        inventoryLogRepo.save(log);

        checkLowStockAndNotify(p);
        return p;
    }

    /**
     * Búsqueda por nombre, descripción y especificaciones (descripción).
     */
    public List<Part> search(String nombre, String descripcion, String especificaciones) {
        return repo.search(nombre, descripcion, especificaciones);
    }

    /**
     * Exportar todos los repuestos (para log se requiere userId).
     */
    public List<Part> exportRepuestos(Long userId) {
        List<Part> list = repo.findAllForExport();
        ImportExportLog log = new ImportExportLog();
        log.setUserId(userId != null ? userId : 0L);
        log.setOperation("EXPORT");
        log.setSuccessCount(list.size());
        log.setErrorCount(0);
        importExportLogRepo.save(log);
        return list;
    }

    /**
     * Importar repuestos desde JSON. Sobreescribe por partNumber. Crea si no existe.
     * @return mapa con successCount, errorCount, detail (mensajes de error)
     */
    public Map<String, Object> importRepuestos(List<Map<String, Object>> items, Long userId, String fileName) {
        int success = 0;
        int errors = 0;
        List<String> detailLines = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            try {
                String partNumber = item.get("partNumber") != null ? item.get("partNumber").toString().trim() : null;
                if (partNumber == null || partNumber.isEmpty()) {
                    errors++;
                    detailLines.add("Fila " + (i + 1) + ": falta partNumber");
                    continue;
                }
                var existing = repo.findByPartNumber(partNumber);
                if (existing.isPresent()) {
                    Part p = existing.get();
                    applyPartFromMap(p, item);
                    repo.save(p);
                } else {
                    Part p = new Part();
                    p.setPartNumber(partNumber);
                    applyPartFromMap(p, item);
                    if (p.getTitle() == null || p.getTitle().isBlank()) p.setTitle(partNumber);
                    if (p.getPrice() == null) p.setPrice(BigDecimal.ZERO);
                    if (p.getStockQuantity() == null) p.setStockQuantity(0);
                    if (p.getLowStockThreshold() == null) p.setLowStockThreshold(5);
                    if (p.getCategoryId() == null) p.setCategoryId(1L);
                    if (p.getBrandId() == null) p.setBrandId(1L);
                    repo.save(p);
                }
                success++;
            } catch (Exception e) {
                errors++;
                detailLines.add("Fila " + (i + 1) + ": " + e.getMessage());
            }
        }

        ImportExportLog log = new ImportExportLog();
        log.setUserId(userId != null ? userId : 0L);
        log.setOperation("IMPORT");
        log.setFileName(fileName);
        log.setSuccessCount(success);
        log.setErrorCount(errors);
        log.setDetail(detailLines.isEmpty() ? null : String.join("\n", detailLines));
        importExportLogRepo.save(log);

        return Map.of("successCount", success, "errorCount", errors, "detail", String.join("\n", detailLines));
    }

    private void applyPartFromMap(Part p, Map<String, Object> item) {
        if (item.get("title") != null) p.setTitle(item.get("title").toString().trim());
        if (item.get("description") != null) p.setDescription(item.get("description").toString().trim());
        if (item.get("categoryId") != null) p.setCategoryId(((Number) item.get("categoryId")).longValue());
        if (item.get("brandId") != null) p.setBrandId(((Number) item.get("brandId")).longValue());
        if (item.get("price") != null) p.setPrice(new BigDecimal(item.get("price").toString()));
        if (item.get("weightLb") != null) p.setWeightLb(new BigDecimal(item.get("weightLb").toString()));
        if (item.get("stockQuantity") != null) p.setStockQuantity(((Number) item.get("stockQuantity")).intValue());
        if (item.get("lowStockThreshold") != null) p.setLowStockThreshold(((Number) item.get("lowStockThreshold")).intValue());
        if (item.get("active") != null) p.setActive(((Number) item.get("active")).intValue());
    }

    /**
     * Carga masiva de inventario por JSON. Body: [{ partNumber, stockQuantity }, ...]
     */
    public Map<String, Object> importInventario(List<Map<String, Object>> items, Long userId, String fileName) {
        int success = 0;
        int errors = 0;
        List<String> detailLines = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            try {
                String partNumber = item.get("partNumber") != null ? item.get("partNumber").toString().trim() : null;
                Integer stock = item.get("stockQuantity") != null ? ((Number) item.get("stockQuantity")).intValue() : null;
                if (partNumber == null || partNumber.isEmpty() || stock == null || stock < 0) {
                    errors++;
                    detailLines.add("Fila " + (i + 1) + ": partNumber y stockQuantity obligatorios (>= 0)");
                    continue;
                }
                var existing = repo.findByPartNumber(partNumber);
                if (existing.isEmpty()) {
                    errors++;
                    detailLines.add("Fila " + (i + 1) + ": repuesto no encontrado: " + partNumber);
                    continue;
                }
                Part p = existing.get();
                p.setStockQuantity(stock);
                repo.save(p);
                success++;
            } catch (Exception e) {
                errors++;
                detailLines.add("Fila " + (i + 1) + ": " + e.getMessage());
            }
        }

        ImportExportLog log = new ImportExportLog();
        log.setUserId(userId != null ? userId : 0L);
        log.setOperation("IMPORT_INVENTORY");
        log.setFileName(fileName);
        log.setSuccessCount(success);
        log.setErrorCount(errors);
        log.setDetail(detailLines.isEmpty() ? null : String.join("\n", detailLines));
        importExportLogRepo.save(log);

        return Map.of("successCount", success, "errorCount", errors, "detail", String.join("\n", detailLines));
    }
}
