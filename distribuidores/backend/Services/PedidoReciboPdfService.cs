using BackendDistribuidores.Data;
using Microsoft.EntityFrameworkCore;
using QuestPDF.Fluent;
using QuestPDF.Helpers;
using QuestPDF.Infrastructure;

namespace BackendDistribuidores.Services;

/// <summary>PDF de recibo del pedido en la distribuidora (resumen unificado).</summary>
public sealed class PedidoReciboPdfService
{
    private readonly AppDbContext _db;

    static PedidoReciboPdfService()
    {
        QuestPDF.Settings.License = LicenseType.Community;
    }

    public PedidoReciboPdfService(AppDbContext db)
    {
        _db = db;
    }

    public async Task<byte[]?> GenerateAsync(long orderId, CancellationToken ct)
    {
        var order = await _db.OrderHeaders.AsNoTracking()
            .FirstOrDefaultAsync(o => o.OrderId == orderId, ct);
        if (order == null) return null;

        var items = await _db.OrderItems.AsNoTracking()
            .Where(i => i.OrderId == orderId)
            .OrderBy(i => i.OrderItemId)
            .ToListAsync(ct);

        var pdf = Document.Create(container =>
        {
            container.Page(page =>
            {
                page.Margin(40);
                page.Size(PageSizes.A4);
                page.DefaultTextStyle(x => x.FontSize(10));

                page.Header()
                    .Text("Recibo de pedido — Distribuidora")
                    .SemiBold()
                    .FontSize(16);

                page.Content().Column(main =>
                {
                    main.Spacing(10);
                    main.Item().Text($"Número: {order.OrderNumber}");
                    main.Item().Text($"Fecha (UTC): {order.CreatedAt:yyyy-MM-dd HH:mm}");
                    main.Item().LineHorizontal(0.5f).LineColor(Colors.Grey.Medium);

                    main.Item().Table(table =>
                    {
                        table.ColumnsDefinition(cols =>
                        {
                            cols.RelativeColumn(4);
                            cols.RelativeColumn(1);
                            cols.RelativeColumn(2);
                            cols.RelativeColumn(2);
                        });

                        static IContainer CellStyle(IContainer c) =>
                            c.BorderBottom(0.5f).BorderColor(Colors.Grey.Lighten2).PaddingVertical(4).PaddingHorizontal(2);

                        table.Header(header =>
                        {
                            header.Cell().Element(CellStyle).Text("Descripción").SemiBold();
                            header.Cell().Element(CellStyle).AlignRight().Text("Cant.").SemiBold();
                            header.Cell().Element(CellStyle).AlignRight().Text("P. unit.").SemiBold();
                            header.Cell().Element(CellStyle).AlignRight().Text("Total").SemiBold();
                        });

                        foreach (var i in items)
                        {
                            var desc = i.TitleSnapshot;
                            if (string.IsNullOrWhiteSpace(desc))
                            {
                                desc = string.Equals(i.LineSource, "FABRICA", StringComparison.OrdinalIgnoreCase)
                                    ? $"Repuesto fábrica #{i.FabricaPartId}"
                                    : $"Repuesto #{i.PartId}";
                            }

                            if (string.Equals(i.LineSource, "FABRICA", StringComparison.OrdinalIgnoreCase))
                                desc += $" (línea fábrica; pedido proveedor #{i.FabricaOrderId})";

                            table.Cell().Element(CellStyle).Text(desc);
                            table.Cell().Element(CellStyle).AlignRight().Text(i.Qty.ToString());
                            table.Cell().Element(CellStyle).AlignRight().Text($"{i.UnitPrice:F2} {order.Currency}");
                            table.Cell().Element(CellStyle).AlignRight().Text($"{i.LineTotal:F2} {order.Currency}");
                        }
                    });

                    main.Item().PaddingTop(12).Column(totals =>
                    {
                        totals.Item().Row(r =>
                        {
                            r.RelativeItem();
                            r.ConstantItem(140).AlignRight().Text($"Subtotal: {order.Subtotal:F2} {order.Currency}");
                        });
                        totals.Item().Row(r =>
                        {
                            r.RelativeItem();
                            r.ConstantItem(140).AlignRight().Text($"Envío: {order.ShippingTotal:F2} {order.Currency}");
                        });
                        if (order.TariffTotal > 0)
                        {
                            totals.Item().Row(r =>
                            {
                                r.RelativeItem();
                                r.ConstantItem(140).AlignRight().Text($"Arancel (import.): {order.TariffTotal:F2} {order.Currency}");
                            });
                        }
                        totals.Item().Row(r =>
                        {
                            r.RelativeItem();
                            r.ConstantItem(140).AlignRight().Text(t => t.Span($"Total: {order.Total:F2} {order.Currency}").SemiBold());
                        });
                    });

                    main.Item().PaddingTop(16).Text(
                        "Para el desglose oficial emitido por cada proveedor (fábrica), use el enlace de recibo de la fábrica cuando aplique.")
                        .FontColor(Colors.Grey.Darken1)
                        .FontSize(8.5f);
                });
            });
        }).GeneratePdf();

        return pdf;
    }
}
