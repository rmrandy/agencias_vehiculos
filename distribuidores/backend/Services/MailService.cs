using System.Net;
using System.Net.Mail;
using System.Text;
using BackendDistribuidores.Models;
using Microsoft.Extensions.Configuration;

namespace BackendDistribuidores.Services;

/// <summary>Envío de correo. Si no hay SMTP configurado, simula el envío (log). Igual que fábrica.</summary>
public class MailService
{
    private readonly string? _host;
    private readonly int _port;
    private readonly string? _user;
    private readonly string? _password;
    private readonly bool _enabled;

    /// <summary>Credenciales por defecto (mismas que fábrica mail.properties) para envío real de correos.</summary>
    private const string DefaultSmtpHost = "smtp.gmail.com";
    private const int DefaultSmtpPort = 587;
    private const string DefaultMailUser = "rr36693904@gmail.com";
    private const string DefaultMailPassword = "oyyg qdla yqga bbho";

    public MailService(IConfiguration configuration)
    {
        _host = FirstNonBlank(
            Environment.GetEnvironmentVariable("MAIL_HOST"),
            configuration["Mail:Host"],
            DefaultSmtpHost
        );
        var portStr = FirstNonBlank(
            Environment.GetEnvironmentVariable("MAIL_PORT"),
            configuration["Mail:Port"]
        );
        _port = int.TryParse(portStr, out var p) ? p : DefaultSmtpPort;
        _user = FirstNonBlank(
            Environment.GetEnvironmentVariable("MAIL_USER"),
            configuration["Mail:User"],
            DefaultMailUser
        );
        _password = FirstNonBlank(
            Environment.GetEnvironmentVariable("MAIL_PASSWORD"),
            configuration["Mail:Password"],
            DefaultMailPassword
        );
        _enabled = !string.IsNullOrWhiteSpace(_host) && !string.IsNullOrWhiteSpace(_user) && !string.IsNullOrWhiteSpace(_password);
    }

    private static string? FirstNonBlank(params string?[] values)
    {
        foreach (var v in values)
        {
            if (!string.IsNullOrWhiteSpace(v)) return v.Trim();
        }
        return null;
    }

    /// <summary>Envía confirmación de pedido al comprador. Si SMTP no está configurado, escribe el contenido en consola.</summary>
    public void SendOrderConfirmation(
        string toEmail,
        string? customerName,
        OrderHeader order,
        IReadOnlyList<(string PartTitle, int Qty, decimal UnitPrice, decimal LineTotal)> items)
    {
        var subject = "Confirmación de pedido #" + order.OrderNumber;
        var htmlBody = BuildOrderEmailHtml(order, items, customerName);

        if (_enabled)
        {
            try
            {
                SendMail(toEmail, subject, htmlBody);
            }
            catch (Exception ex)
            {
                Console.WriteLine("[MailService] Error enviando correo: " + ex.Message);
                LogSimulatedEmail(toEmail, subject, htmlBody);
            }
        }
        else
        {
            LogSimulatedEmail(toEmail, subject, htmlBody);
        }
    }

    /// <summary>Envía correo al cliente cuando se actualiza el estado del pedido (estado, comentario, tracking, ETA).</summary>
    public void SendOrderStatusUpdate(
        string toEmail,
        string? customerName,
        string orderNumber,
        string newStatus,
        string? comment,
        string? trackingNumber,
        int? etaDays)
    {
        var statusLabel = FormatStatusLabel(newStatus);
        var subject = "Actualización de pedido #" + orderNumber;
        var htmlBody = BuildStatusUpdateEmailHtml(orderNumber, statusLabel, customerName, comment, trackingNumber, etaDays);

        if (_enabled)
        {
            try
            {
                SendMail(toEmail, subject, htmlBody);
            }
            catch (Exception ex)
            {
                Console.WriteLine("[MailService] Error enviando correo de actualización: " + ex.Message);
                LogSimulatedEmail(toEmail, subject, htmlBody);
            }
        }
        else
        {
            LogSimulatedEmail(toEmail, subject, htmlBody);
        }
    }

    private static string FormatStatusLabel(string status)
    {
        if (string.IsNullOrEmpty(status)) return status;
        return status.ToUpperInvariant() switch
        {
            "INITIATED" => "Iniciado",
            "CONFIRMED" => "Confirmado",
            "IN_PREPARATION" or "PREPARING" => "En preparación",
            "SHIPPED" => "Enviado",
            "DELIVERED" => "Entregado",
            "CANCELLED" => "Cancelado",
            _ => status
        };
    }

    private static string BuildStatusUpdateEmailHtml(
        string orderNumber,
        string statusLabel,
        string? customerName,
        string? comment,
        string? trackingNumber,
        int? etaDays)
    {
        var sb = new StringBuilder();
        sb.Append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: sans-serif;'>");
        sb.Append("<h2>Actualización de tu pedido</h2>");
        sb.Append("<p>Hola");
        if (!string.IsNullOrWhiteSpace(customerName)) sb.Append(" ").Append(Escape(customerName));
        sb.Append(",</p>");
        sb.Append("<p>El estado de tu pedido <strong>").Append(Escape(orderNumber)).Append("</strong> ha cambiado.</p>");
        sb.Append("<p><strong>Nuevo estado:</strong> ").Append(Escape(statusLabel)).Append("</p>");
        if (!string.IsNullOrWhiteSpace(comment))
            sb.Append("<p><strong>Comentario:</strong> ").Append(Escape(comment)).Append("</p>");
        if (!string.IsNullOrWhiteSpace(trackingNumber))
            sb.Append("<p><strong>Número de seguimiento:</strong> ").Append(Escape(trackingNumber)).Append("</p>");
        if (etaDays.HasValue && etaDays.Value > 0)
            sb.Append("<p><strong>Tiempo estimado de entrega:</strong> ").Append(etaDays.Value).Append(" días</p>");
        sb.Append("<p>Puedes ver el detalle en <strong>Mis Pedidos</strong> en la aplicación.</p>");
        sb.Append("<p>— Distribuidores Agencias Vehículos</p></body></html>");
        return sb.ToString();
    }

    private void LogSimulatedEmail(string to, string subject, string htmlBody)
    {
        Console.WriteLine("---------- CORREO SIMULADO ----------");
        Console.WriteLine("Para: " + to);
        Console.WriteLine("Asunto: " + subject);
        Console.WriteLine("Contenido (HTML):");
        Console.WriteLine(htmlBody);
        Console.WriteLine("------------------------------------");
    }

    private void SendMail(string to, string subject, string htmlContent)
    {
        using var client = new SmtpClient(_host, _port)
        {
            EnableSsl = true,
            Credentials = new NetworkCredential(_user, _password ?? "")
        };
        var msg = new MailMessage
        {
            From = new MailAddress(_user!, "Distribuidores - Agencias Vehículos", Encoding.UTF8),
            Subject = subject,
            Body = htmlContent,
            IsBodyHtml = true,
            BodyEncoding = Encoding.UTF8
        };
        msg.To.Add(to);
        client.Send(msg);
    }

    private static string BuildOrderEmailHtml(
        OrderHeader order,
        IReadOnlyList<(string PartTitle, int Qty, decimal UnitPrice, decimal LineTotal)> items,
        string? customerName)
    {
        var sb = new StringBuilder();
        sb.Append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family: sans-serif;'>");
        sb.Append("<h2>Gracias por tu compra</h2>");
        sb.Append("<p>Hola");
        if (!string.IsNullOrWhiteSpace(customerName)) sb.Append(" ").Append(Escape(customerName));
        sb.Append(",</p>");
        sb.Append("<p>Tu pedido ha sido registrado correctamente.</p>");
        sb.Append("<p><strong>Número de pedido:</strong> ").Append(Escape(order.OrderNumber)).Append("</p>");
        sb.Append("<table border='1' cellpadding='8' style='border-collapse: collapse;'>");
        sb.Append("<thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unit.</th><th>Total</th></tr></thead><tbody>");
        foreach (var item in items)
        {
            sb.Append("<tr><td>").Append(Escape(item.PartTitle))
                .Append("</td><td>").Append(item.Qty)
                .Append("</td><td>").Append(item.UnitPrice.ToString("F2"))
                .Append("</td><td>").Append(item.LineTotal.ToString("F2"))
                .Append("</td></tr>");
        }
        sb.Append("</tbody></table>");
        sb.Append("<p><strong>Subtotal:</strong> ").Append(order.Subtotal.ToString("F2")).Append(" ").Append(order.Currency).Append("</p>");
        sb.Append("<p><strong>Envío:</strong> ").Append(order.ShippingTotal.ToString("F2")).Append(" ").Append(order.Currency).Append("</p>");
        sb.Append("<p><strong>Total:</strong> ").Append(order.Total.ToString("F2")).Append(" ").Append(order.Currency).Append("</p>");
        sb.Append("<p>Puedes ver el detalle y seguimiento en <strong>Mis Pedidos</strong> en la aplicación.</p>");
        sb.Append("<p>— Distribuidores Agencias Vehículos</p></body></html>");
        return sb.ToString();
    }

    private static string Escape(string? s)
    {
        if (string.IsNullOrEmpty(s)) return "";
        return s
            .Replace("&", "&amp;")
            .Replace("<", "&lt;")
            .Replace(">", "&gt;")
            .Replace("\"", "&quot;");
    }
}
