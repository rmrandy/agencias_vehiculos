namespace BackendDistribuidores.Services;

/// <summary>Reglas compartidas de transición de estado del pedido maestro (local + webhook desde fábrica).</summary>
public static class PedidoEstadoRules
{
    /// <summary>Orden de estados: solo se permite avanzar (no retroceder). CANCELLED y DELIVERED son finales.</summary>
    public static bool CanAdvanceTo(string currentStatus, string newStatus)
    {
        var current = (currentStatus ?? "INITIATED").Trim().ToUpperInvariant();
        var next = (newStatus ?? "").Trim().ToUpperInvariant();
        if (current == next)
            return false;

        var order = new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase)
        {
            ["INITIATED"] = 0,
            ["CONFIRMED"] = 1,
            ["PREPARING"] = 2,
            ["IN_PREPARATION"] = 2,
            ["SHIPPED"] = 3,
            ["DELIVERED"] = 4,
            ["CANCELLED"] = 99
        };
        if (!order.TryGetValue(next, out var newOrder))
            return false;
        if (!order.TryGetValue(current, out var currentOrder))
            return true;
        if (currentOrder == 99)
            return false;
        if (next == "CANCELLED")
            return currentOrder < 4; // no cancelar si ya entregado (alineado con reglas de la fábrica)
        return newOrder >= currentOrder;
    }

    /// <summary>Alinea nombres de estado del API de fábrica con los del pedido maestro en distribuidora.</summary>
    public static string NormalizeFabricaStatus(string? status)
    {
        var u = (status ?? "").Trim().ToUpperInvariant();
        if (u == "CONFIRMED" || u == "IN_PREPARATION")
            return "PREPARING";
        return u;
    }
}
