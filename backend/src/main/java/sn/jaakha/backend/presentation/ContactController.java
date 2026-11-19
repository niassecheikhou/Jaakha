package sn.jaakha.backend.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.jaakha.backend.application.ContactMessageService;
import sn.jaakha.backend.application.CurrentUserService;
import sn.jaakha.backend.application.dto.ContactMessageRequest;
import sn.jaakha.backend.application.dto.ContactMessageResponse;

@RestController
public class ContactController {
    private final ContactMessageService contactMessageService;
    private final CurrentUserService currentUserService;

    public ContactController(ContactMessageService contactMessageService, CurrentUserService currentUserService) {
        this.contactMessageService = contactMessageService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/api/contact/messages")
    public ContactMessageResponse create(@Valid @RequestBody ContactMessageRequest request) {
        return contactMessageService.create(request);
    }

    @GetMapping("/api/admin/contact-messages")
    public List<ContactMessageResponse> messages(HttpServletRequest request) {
        currentUserService.requireAdmin(request);
        return contactMessageService.findAll();
    }

    @GetMapping("/api/admin/contact-messages/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            @RequestParam(defaultValue = "csv") String format
    ) {
        currentUserService.requireAdmin(request);
        boolean excel = "excel".equalsIgnoreCase(format) || "xls".equalsIgnoreCase(format);
        byte[] content = excel ? contactMessageService.exportExcel() : contactMessageService.exportCsv();
        String filename = excel ? "messages-contact-jaankha.xls" : "messages-contact-jaankha.csv";
        MediaType mediaType = excel
                ? MediaType.parseMediaType("application/vnd.ms-excel")
                : MediaType.parseMediaType("text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
