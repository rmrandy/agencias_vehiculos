package com.agencias.backend.service;

import com.agencias.backend.model.OrderHeader;
import com.agencias.backend.model.OrderItem;
import com.agencias.backend.model.Part;
import com.agencias.backend.repository.OrderItemRepository;
import com.agencias.backend.repository.OrderRepository;
import com.agencias.backend.repository.PartRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.EntityManagerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Genera el PDF del recibo de un pedido.
 */
public class ReciboPdfService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final PartRepository partRepo;

    public ReciboPdfService(EntityManagerFactory emf) {
        this.orderRepo = new OrderRepository(emf);
        this.itemRepo = new OrderItemRepository(emf);
        this.partRepo = new PartRepository(emf);
    }

    /**
     * Genera el PDF del recibo para el pedido dado.
     * @return bytes del PDF o null si el pedido no existe
     */
    public byte[] generarReciboPdf(Long orderId) {
        OrderHeader order = orderRepo.findById(orderId).orElse(null);
        if (order == null) {
            return null;
        }
        List<OrderItem> items = itemRepo.findByOrderId(orderId);

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            doc.add(new Paragraph("RECIBO DE COMPRA", titleFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Pedido: " + order.getOrderNumber(), normalFont));
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es"));
            String fecha = order.getCreatedAt() != null ? sdf.format(order.getCreatedAt()) : "—";
            doc.add(new Paragraph("Fecha: " + fecha, normalFont));
            doc.add(new Paragraph(" "));

            // Tabla de ítems
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{4f, 1.5f, 2f, 2f});
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            table.addCell(cell("Repuesto / No. Parte", true));
            table.addCell(cell("Cant.", true));
            table.addCell(cell("P. unitario", true));
            table.addCell(cell("Total línea", true));

            for (OrderItem item : items) {
                String partInfo = item.getPartId().toString();
                Optional<Part> partOpt = partRepo.findById(item.getPartId());
                if (partOpt.isPresent()) {
                    Part p = partOpt.get();
                    partInfo = p.getTitle() + " (" + p.getPartNumber() + ")";
                }
                table.addCell(cell(partInfo, false));
                table.addCell(cell(String.valueOf(item.getQty()), false));
                table.addCell(cell(formatMoney(item.getUnitPrice()), false));
                table.addCell(cell(formatMoney(item.getLineTotal()), false));
            }

            doc.add(table);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Subtotal: " + formatMoney(order.getSubtotal()), normalFont));
            if (order.getShippingTotal() != null && order.getShippingTotal().compareTo(BigDecimal.ZERO) > 0) {
                doc.add(new Paragraph("Envío: " + formatMoney(order.getShippingTotal()), normalFont));
            }
            doc.add(new Paragraph("TOTAL: " + formatMoney(order.getTotal()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Gracias por su compra.", smallFont));

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    private static PdfPCell cell(String text, boolean header) {
        PdfPCell c = new PdfPCell(new Phrase(text, header ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10) : FontFactory.getFont(FontFactory.HELVETICA, 9)));
        if (header) {
            c.setBackgroundColor(new java.awt.Color(240, 240, 240));
        }
        c.setPadding(4f);
        return c;
    }

    private static String formatMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format(Locale.US, "%.2f", value);
    }
}
