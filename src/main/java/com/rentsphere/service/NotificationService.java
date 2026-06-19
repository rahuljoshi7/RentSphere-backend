package com.rentsphere.service;

import com.rentsphere.entity.*;
import com.rentsphere.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender         mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── Rent Due ──────────────────────────────────────────────────────────────

    @Async
    @Transactional
    public void sendRentDueNotification(Payment payment) {
        User user = payment.getTenant().getUser();
        String subject = "Rent Due – " + payment.getProperty().getName();
        String body = String.format("""
            Dear %s,

            This is a reminder that your rent payment of ₹%s for %s is due on %s.

            Property : %s
            Month    : %d/%d
            Amount   : ₹%s

            Please ensure timely payment to avoid late fees.

            Regards,
            RentSphere Team
            """,
            user.getFullName(),
            payment.getAmountDue(),
            payment.getAgreement().getProperty().getName(),
            payment.getDueDate().format(DATE_FMT),
            payment.getAgreement().getProperty().getName(),
            payment.getMonth(), payment.getYear(),
            payment.getAmountDue()
        );

        persist(user, Notification.NotificationType.RENT_DUE, subject, body);
        sendEmail(user.getEmail(), subject, body);
    }

    // ── Agreement Expiry ──────────────────────────────────────────────────────

    @Async
    @Transactional
    public void sendAgreementExpiryNotification(RentalAgreement agreement) {
        User tenantUser = agreement.getTenant().getUser();
        User ownerUser  = agreement.getProperty().getOwner();

        String subject = "Rental Agreement Expiring – " + agreement.getProperty().getName();
        String body = String.format("""
            Dear %s,

            Your rental agreement for %s is expiring on %s.

            Please contact your property owner/manager to discuss renewal.

            Property : %s
            End Date : %s

            Regards,
            RentSphere Team
            """,
            tenantUser.getFullName(),
            agreement.getProperty().getName(),
            agreement.getEndDate().format(DATE_FMT),
            agreement.getProperty().getName(),
            agreement.getEndDate().format(DATE_FMT)
        );

        persist(tenantUser, Notification.NotificationType.AGREEMENT_EXPIRY, subject, body);
        persist(ownerUser,  Notification.NotificationType.AGREEMENT_EXPIRY, subject, body);
        sendEmail(tenantUser.getEmail(), subject, body);
        sendEmail(ownerUser.getEmail(),  subject, body);
    }

    // ── Maintenance Update ────────────────────────────────────────────────────

    @Async
    @Transactional
    public void sendMaintenanceUpdateNotification(MaintenanceRequest request) {
        User tenantUser = request.getTenant().getUser();
        User ownerUser  = request.getProperty().getOwner();

        String subject = "Maintenance Request Update – " + request.getTitle();
        String body = String.format("""
            Maintenance request status update:

            Title    : %s
            Property : %s
            Status   : %s
            Priority : %s

            Regards,
            RentSphere Team
            """,
            request.getTitle(),
            request.getProperty().getName(),
            request.getStatus(),
            request.getPriority()
        );

        persist(tenantUser, Notification.NotificationType.MAINTENANCE_UPDATE, subject, body);
        persist(ownerUser,  Notification.NotificationType.MAINTENANCE_UPDATE, subject, body);
        sendEmail(tenantUser.getEmail(), subject, body);
    }

    // ── Tenant Registration ───────────────────────────────────────────────────

    @Async
    public void sendTenantRegistrationNotification(User user) {
        String subject = "Welcome to RentSphere!";
        String body = String.format("""
            Dear %s,

            Welcome to RentSphere! Your tenant account has been successfully created.

            You can now:
            - View your rental agreement
            - Track rent payments
            - Raise maintenance requests
            - Manage facility complaints

            Login at: https://rentsphere.onrender.com

            Regards,
            RentSphere Team
            """,
            user.getFullName()
        );

        persist(user, Notification.NotificationType.TENANT_REGISTRATION, subject, body);
        sendEmail(user.getEmail(), subject, body);
    }

    // ── In-app queries ────────────────────────────────────────────────────────

    public org.springframework.data.domain.Page<Notification> getForUser(
            Long userId, org.springframework.data.domain.Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.markAsRead();
            notificationRepository.save(n);
        });
    }

    // ── Send unsent batch (scheduler fallback) ────────────────────────────────

    @Transactional
    public void flushUnsent() {
        List<Notification> unsent = notificationRepository.findUnsent();
        unsent.forEach(n -> {
            sendEmail(n.getUser().getEmail(), n.getSubject(), n.getBody());
            n.markAsSent();
        });
        if (!unsent.isEmpty()) notificationRepository.saveAll(unsent);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void persist(User user, Notification.NotificationType type, String subject, String body) {
        Notification notification = Notification.builder()
            .user(user)
            .type(type)
            .subject(subject)
            .body(body)
            .build();
        notificationRepository.save(notification);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
