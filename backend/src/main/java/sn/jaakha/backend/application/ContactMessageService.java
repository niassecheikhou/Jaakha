package sn.jaakha.backend.application;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jaakha.backend.application.dto.ContactMessageRequest;
import sn.jaakha.backend.application.dto.ContactMessageResponse;
import sn.jaakha.backend.infrastructure.persistence.ContactMessageRepository;

@Service
public class ContactMessageService {
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ContactMessageRepository contactMessageRepository;
    private final MailService mailService;

    public ContactMessageService(ContactMessageRepository contactMessageRepository, MailService mailService) {
        this.contactMessageRepository = contactMessageRepository;
        this.mailService = mailService;
    }

    @Transactional
    public ContactMessageResponse create(ContactMessageRequest request) {
        ContactMessageResponse message = contactMessageRepository.create(
                UUID.randomUUID(),
                clean(request.name()),
                clean(request.email()).toLowerCase(),
                cleanNullable(request.phone()),
                cleanNullable(request.company()),
                clean(request.message())
        );
        mailService.sendContactAcknowledgement(message);
        return message;
    }

    @Transactional(readOnly = true)
    public List<ContactMessageResponse> findAll() {
        return contactMessageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("Date,Nom,Email,Telephone,Entreprise,Message\r\n");
        for (ContactMessageResponse message : findAll()) {
            builder
                    .append(csv(message.createdAt().format(EXPORT_DATE_FORMAT))).append(',')
                    .append(csv(message.name())).append(',')
                    .append(csv(message.email())).append(',')
                    .append(csv(message.phone())).append(',')
                    .append(csv(message.company())).append(',')
                    .append(csv(message.message())).append("\r\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel() {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <html>
                <head><meta charset="UTF-8"></head>
                <body>
                <table border="1">
                <thead><tr><th>Date</th><th>Nom</th><th>Email</th><th>Telephone</th><th>Entreprise</th><th>Message</th></tr></thead>
                <tbody>
                """);
        for (ContactMessageResponse message : findAll()) {
            builder
                    .append("<tr><td>").append(html(message.createdAt().format(EXPORT_DATE_FORMAT))).append("</td>")
                    .append("<td>").append(html(message.name())).append("</td>")
                    .append("<td>").append(html(message.email())).append("</td>")
                    .append("<td>").append(html(message.phone())).append("</td>")
                    .append("<td>").append(html(message.company())).append("</td>")
                    .append("<td>").append(html(message.message())).append("</td></tr>");
        }
        builder.append("""
                </tbody>
                </table>
                </body>
                </html>
                """);
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanNullable(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private String html(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
