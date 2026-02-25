package com.agencias.backend.service;

import com.agencias.backend.controller.dto.ImportExportLogDto;
import com.agencias.backend.controller.dto.InventoryLogDto;
import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.ImportExportLog;
import com.agencias.backend.model.InventoryLog;
import com.agencias.backend.model.Part;
import com.agencias.backend.repository.ImportExportLogRepository;
import com.agencias.backend.repository.InventoryLogRepository;
import com.agencias.backend.repository.PartRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReporteriaService {
    private final ImportExportLogRepository importExportLogRepo;
    private final InventoryLogRepository inventoryLogRepo;
    private final UserService userService;
    private final PartRepository partRepository;

    public ReporteriaService(EntityManagerFactory emf) {
        this.importExportLogRepo = new ImportExportLogRepository(emf);
        this.inventoryLogRepo = new InventoryLogRepository(emf);
        this.userService = new UserService(emf);
        this.partRepository = new PartRepository(emf);
    }

    public List<ImportExportLogDto> listImportExportLog(Integer limit, String operation) {
        List<ImportExportLog> list = importExportLogRepo.findRecent(limit != null ? limit : 200, operation);
        List<Long> userIds = list.stream().map(ImportExportLog::getUserId).distinct().toList();
        Map<Long, String> userNames = loadUserDisplayNames(userIds);

        List<ImportExportLogDto> result = new ArrayList<>();
        for (ImportExportLog log : list) {
            ImportExportLogDto dto = new ImportExportLogDto();
            dto.setLogId(log.getLogId());
            dto.setUserId(log.getUserId());
            dto.setUserDisplayName(userNames.getOrDefault(log.getUserId(), "ID " + log.getUserId()));
            dto.setCreatedAt(log.getCreatedAt());
            dto.setOperation(log.getOperation());
            dto.setFileName(log.getFileName());
            dto.setSuccessCount(log.getSuccessCount());
            dto.setErrorCount(log.getErrorCount());
            dto.setDetail(log.getDetail());
            result.add(dto);
        }
        return result;
    }

    public List<InventoryLogDto> listInventoryLog(Integer limit, Long partId, Long userId) {
        List<InventoryLog> list = inventoryLogRepo.findRecent(limit != null ? limit : 200, partId, userId);
        List<Long> userIds = list.stream().map(InventoryLog::getUserId).distinct().toList();
        List<Long> partIds = list.stream().map(InventoryLog::getPartId).distinct().toList();
        Map<Long, String> userNames = loadUserDisplayNames(userIds);
        Map<Long, Part> parts = partIds.stream()
            .map(id -> partRepository.findById(id).orElse(null))
            .filter(p -> p != null)
            .collect(Collectors.toMap(Part::getPartId, p -> p));

        List<InventoryLogDto> result = new ArrayList<>();
        for (InventoryLog log : list) {
            InventoryLogDto dto = new InventoryLogDto();
            dto.setLogId(log.getLogId());
            dto.setPartId(log.getPartId());
            Part p = parts.get(log.getPartId());
            dto.setPartTitle(p != null ? p.getTitle() : null);
            dto.setPartNumber(p != null ? p.getPartNumber() : null);
            dto.setUserId(log.getUserId());
            dto.setUserDisplayName(userNames.getOrDefault(log.getUserId(), "ID " + log.getUserId()));
            dto.setCreatedAt(log.getCreatedAt());
            dto.setQuantityAdded(log.getQuantityAdded());
            dto.setPreviousQuantity(log.getPreviousQuantity());
            dto.setNewQuantity(log.getNewQuantity());
            result.add(dto);
        }
        return result;
    }

    private Map<Long, String> loadUserDisplayNames(List<Long> userIds) {
        Map<Long, String> map = new java.util.HashMap<>();
        for (Long uid : userIds) {
            AppUser u = userService.getById(uid);
            if (u != null) {
                String name = (u.getFullName() != null && !u.getFullName().isBlank()) ? u.getFullName() : u.getEmail();
                map.put(uid, name);
            }
        }
        return map;
    }
}
