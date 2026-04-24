package com.agencias.backend.service;

import org.junit.jupiter.api.Test;

/**
 * Sin URL de webhook configurada, la notificación no lanza (no-op seguro).
 */
class DistributorOrderWebhookNotifierTest {

    @Test
    void notifyOrderStatusAsync_noUrl_doesNotThrow() {
        DistributorOrderWebhookNotifier.notifyOrderStatusAsync(99L, "SHIPPED", null, null, null);
    }
}
