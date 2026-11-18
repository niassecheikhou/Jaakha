package sn.jaakha.backend.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import sn.jaakha.backend.application.dto.ContactMessageResponse;
import sn.jaakha.backend.application.dto.UserResponse;

@Service
public class MailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    private final String from;
    private final String frontendUrl;
    private final JavaMailSender mailSender;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:${MAIL_FROM:no-reply@jaankha.sn}}") String from,
            @Value("${app.frontend-url:${APP_FRONTEND_URL:http://localhost:5173}}") String frontendUrl
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Email de bienvenue, sans mot de passe. Si passwordSetupUrl est fourni (compte cree par un admin),
     * le bouton invite a choisir son mot de passe ; sinon il renvoie vers la connexion.
     */
    public void sendAccountCreated(UserResponse user, String passwordSetupUrl) {
        if (user.email() == null || user.email().isBlank()) {
            return;
        }

        boolean needsPasswordSetup = passwordSetupUrl != null;
        String actionUrl = needsPasswordSetup ? passwordSetupUrl : loginUrl();
        String actionLabel = needsPasswordSetup ? "Choisir mon mot de passe" : "Se connecter";
        String intro = needsPasswordSetup
                ? "Un administrateur a cree votre compte Jaankha. Choisissez votre mot de passe pour vous connecter."
                : "Votre compte Jaankha a ete cree. Connectez-vous avec le mot de passe que vous avez choisi.";
        String expiryNote = needsPasswordSetup
                ? "Ce lien expire bientot. Une fois expire, utilisez \"Mot de passe oublie\" sur la page de connexion."
                : "";

        String plainText = """
                Bonjour %s,

                %s

                Identifiant: %s ou %s

                %s

                %s
                Completez ensuite votre dossier pour permettre la validation du compte.

                Equipe Jaankha
                """.formatted(
                user.fullName(),
                intro,
                user.email(),
                user.phone(),
                actionUrl,
                expiryNote
        );

        String html = """
                <!doctype html>
                <html>
                  <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,sans-serif;color:#212e53;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5f7fb;padding:24px;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:12px;padding:28px;border:1px solid #dfe6ef;">
                            <tr>
                              <td>
                                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.25;color:#212e53;">Votre compte Jaankha est pret</h1>
                                <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">Bonjour %s,</p>
                                <p style="margin:0 0 18px;font-size:16px;line-height:1.6;">%s</p>
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:0 0 22px;border-collapse:collapse;background:#f5f7fb;border-radius:10px;overflow:hidden;">
                                  <tr>
                                    <td style="padding:12px 14px;font-size:14px;color:#5f6b85;border-bottom:1px solid #dfe6ef;">Email</td>
                                    <td style="padding:12px 14px;font-size:14px;color:#212e53;font-weight:700;border-bottom:1px solid #dfe6ef;">%s</td>
                                  </tr>
                                  <tr>
                                    <td style="padding:12px 14px;font-size:14px;color:#5f6b85;">Telephone</td>
                                    <td style="padding:12px 14px;font-size:14px;color:#212e53;font-weight:700;">%s</td>
                                  </tr>
                                </table>
                                <p style="margin:0 0 24px;">
                                  <a href="%s" style="display:inline-block;background:#4a919e;color:#ffffff;text-decoration:none;font-weight:700;font-size:16px;padding:14px 22px;border-radius:8px;">%s</a>
                                </p>
                                <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#5f6b85;">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :</p>
                                <p style="margin:0 0 22px;font-size:14px;line-height:1.6;word-break:break-all;">
                                  <a href="%s" style="color:#4a919e;">%s</a>
                                </p>
                                <p style="margin:0 0 18px;font-size:14px;line-height:1.6;color:#5f6b85;">%s Completez ensuite votre dossier pour permettre la validation du compte.</p>
                                <p style="margin:0;font-size:15px;line-height:1.6;color:#212e53;">Equipe Jaankha</p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(
                escapeHtml(user.fullName()),
                escapeHtml(intro),
                escapeHtml(user.email()),
                escapeHtml(user.phone()),
                escapeHtml(actionUrl),
                escapeHtml(actionLabel),
                escapeHtml(actionUrl),
                escapeHtml(actionUrl),
                escapeHtml(expiryNote)
        );

        sendHtml(user.email(), "Votre compte Jaankha est pret", plainText, html);
    }

    public void sendPasswordResetLink(UserResponse user, String resetUrl) {
        if (user.email() == null || user.email().isBlank()) {
            return;
        }

        String plainText = """
                Bonjour %s,

                Vous avez demande la reinitialisation de votre mot de passe Jaankha.
                Cliquez sur ce lien pour choisir un nouveau mot de passe:

                %s

                Ce lien expire bientot. Si vous n'etes pas a l'origine de cette demande, ignorez cet email.

                Equipe Jaankha
                """.formatted(user.fullName(), resetUrl);

        String html = """
                <!doctype html>
                <html>
                  <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,sans-serif;color:#212e53;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5f7fb;padding:24px;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:12px;padding:28px;border:1px solid #dfe6ef;">
                            <tr>
                              <td>
                                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.25;color:#212e53;">Reinitialisation du mot de passe</h1>
                                <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">Bonjour %s,</p>
                                <p style="margin:0 0 22px;font-size:16px;line-height:1.6;">Vous avez demande la reinitialisation de votre mot de passe Jaankha. Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe.</p>
                                <p style="margin:0 0 24px;">
                                  <a href="%s" style="display:inline-block;background:#4a919e;color:#ffffff;text-decoration:none;font-weight:700;font-size:16px;padding:14px 22px;border-radius:8px;">Reinitialiser mon mot de passe</a>
                                </p>
                                <p style="margin:0 0 8px;font-size:14px;line-height:1.6;color:#5f6b85;">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :</p>
                                <p style="margin:0 0 22px;font-size:14px;line-height:1.6;word-break:break-all;">
                                  <a href="%s" style="color:#4a919e;">%s</a>
                                </p>
                                <p style="margin:0 0 18px;font-size:14px;line-height:1.6;color:#5f6b85;">Ce lien expire bientot. Si vous n'etes pas a l'origine de cette demande, ignorez cet email.</p>
                                <p style="margin:0;font-size:15px;line-height:1.6;color:#212e53;">Equipe Jaankha</p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(
                escapeHtml(user.fullName()),
                escapeHtml(resetUrl),
                escapeHtml(resetUrl),
                escapeHtml(resetUrl)
        );

        sendHtml(user.email(), "Reinitialisation de votre mot de passe Jaankha", plainText, html);
    }

    public void sendContactAcknowledgement(ContactMessageResponse contactMessage) {
        if (contactMessage.email() == null || contactMessage.email().isBlank()) {
            return;
        }

        String plainText = """
                Bonjour %s,

                Nous avons bien recu votre message.
                Notre equipe va l'etudier et reviendra vers vous rapidement.

                Rappel de votre demande:
                %s

                Equipe Jaankha
                """.formatted(contactMessage.name(), contactMessage.message());

        String html = """
                <!doctype html>
                <html>
                  <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,sans-serif;color:#212e53;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5f7fb;padding:24px;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:12px;padding:28px;border:1px solid #dfe6ef;">
                            <tr>
                              <td>
                                <h1 style="margin:0 0 16px;font-size:24px;line-height:1.25;color:#212e53;">Message bien recu</h1>
                                <p style="margin:0 0 14px;font-size:16px;line-height:1.6;">Bonjour %s,</p>
                                <p style="margin:0 0 18px;font-size:16px;line-height:1.6;">Nous avons bien recu votre message. Notre equipe va l'etudier et reviendra vers vous rapidement.</p>
                                <div style="margin:0 0 22px;padding:16px;border-radius:10px;background:#f5f7fb;border-left:4px solid #4a919e;">
                                  <p style="margin:0 0 8px;font-size:14px;font-weight:700;color:#212e53;">Rappel de votre demande</p>
                                  <p style="margin:0;font-size:14px;line-height:1.6;color:#5f6b85;">%s</p>
                                </div>
                                <p style="margin:0;font-size:15px;line-height:1.6;color:#212e53;">Equipe Jaankha</p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(
                escapeHtml(contactMessage.name()),
                escapeHtml(contactMessage.message()).replace("\n", "<br>")
        );

        sendHtml(contactMessage.email(), "Nous avons bien recu votre message", plainText, html);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException exception) {
            LOGGER.warn("Email Jaankha non envoye a {}: {}", to, exception.getMessage());
        }
    }

    private String loginUrl() {
        String cleanFrontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        return cleanFrontendUrl + "/login";
    }

    private void sendHtml(String to, String subject, String plainText, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            LOGGER.warn("Email Jaankha non envoye a {}: {}", to, exception.getMessage());
        }
    }

    private String escapeHtml(String value) {
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
