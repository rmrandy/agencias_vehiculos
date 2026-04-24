package com.agencias.backend.controller.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.agencias.backend.model.AppUser;
import com.agencias.backend.model.Role;

public class UsuarioResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String status;
    private Date createdAt;
    private List<String> roles;
    private Boolean isEnterprise;
    private BigDecimal enterpriseDiscountPercent;

    public static UsuarioResponse from(AppUser u) {
        UsuarioResponse r = new UsuarioResponse();
        r.setUserId(u.getUserId());
        r.setEmail(u.getEmail());
        r.setFullName(u.getFullName());
        r.setPhone(u.getPhone());
        r.setStatus(u.getStatus());
        r.setCreatedAt(u.getCreatedAt());
        r.setRoles(u.getRoles() != null
            ? u.getRoles().stream().map(Role::getName).collect(Collectors.toList())
            : List.of());
        return r;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public Boolean getIsEnterprise() { return isEnterprise; }
    public void setIsEnterprise(Boolean isEnterprise) { this.isEnterprise = isEnterprise; }
    public BigDecimal getEnterpriseDiscountPercent() { return enterpriseDiscountPercent; }
    public void setEnterpriseDiscountPercent(BigDecimal enterpriseDiscountPercent) { this.enterpriseDiscountPercent = enterpriseDiscountPercent; }
}
