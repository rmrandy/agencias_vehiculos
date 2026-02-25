package com.agencias.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Perfil empresarial: usuario con rol ENTERPRISE.
 * Incluye api_key para acceso por API.
 */
@Entity
@Table(name = "ENTERPRISE_PROFILE")
public class EnterpriseProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enterprise_profile_seq")
    @SequenceGenerator(name = "enterprise_profile_seq", sequenceName = "ENTERPRISE_PROFILE_SEQ", allocationSize = 1)
    @Column(name = "enterprise_id")
    private Long enterpriseId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "default_shipping_address_id")
    private Long defaultShippingAddressId;

    @Column(name = "default_billing_address_id")
    private Long defaultBillingAddressId;

    @Column(name = "default_card_token", length = 255)
    private String defaultCardToken;

    /** Dirección de envío por defecto (texto libre o JSON). */
    @Column(name = "default_address_text", length = 1000)
    private String defaultAddressText;

    /** Últimos 4 dígitos de la tarjeta por defecto (solo para mostrar). */
    @Column(name = "default_card_last4", length = 10)
    private String defaultCardLast4;

    @Column(name = "delivery_window", length = 500)
    private String deliveryWindow;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    public EnterpriseProfile() {
    }

    public Long getEnterpriseId() { return enterpriseId; }
    public void setEnterpriseId(Long enterpriseId) { this.enterpriseId = enterpriseId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Long getDefaultShippingAddressId() { return defaultShippingAddressId; }
    public void setDefaultShippingAddressId(Long defaultShippingAddressId) { this.defaultShippingAddressId = defaultShippingAddressId; }
    public Long getDefaultBillingAddressId() { return defaultBillingAddressId; }
    public void setDefaultBillingAddressId(Long defaultBillingAddressId) { this.defaultBillingAddressId = defaultBillingAddressId; }
    public String getDefaultCardToken() { return defaultCardToken; }
    public void setDefaultCardToken(String defaultCardToken) { this.defaultCardToken = defaultCardToken; }
    public String getDefaultAddressText() { return defaultAddressText; }
    public void setDefaultAddressText(String defaultAddressText) { this.defaultAddressText = defaultAddressText; }
    public String getDefaultCardLast4() { return defaultCardLast4; }
    public void setDefaultCardLast4(String defaultCardLast4) { this.defaultCardLast4 = defaultCardLast4; }
    public String getDeliveryWindow() { return deliveryWindow; }
    public void setDeliveryWindow(String deliveryWindow) { this.deliveryWindow = deliveryWindow; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
}
