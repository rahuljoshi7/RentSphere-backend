package com.rentsphere.service;

import com.rentsphere.entity.RentalAgreement;
import com.rentsphere.repository.RentalAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final PaymentService            paymentService;
    private final RentalAgreementService    agreementService;
    private final NotificationService       notificationService;
    private final RentalAgreementRepository agreementRepository;

    @Value("${app.scheduler.rent-reminder-cron}")
    private String rentCron; // used for documentation; actual cron is in @Scheduled

    /**
     * 1st of every month at 9 AM — generate monthly rent for all active agreements.
     */
    @Scheduled(cron = "${app.scheduler.rent-reminder-cron}")
    public void generateMonthlyRent() {
        LocalDate now = LocalDate.now();
        log.info("[Scheduler] Generating monthly rent for {}/{}", now.getMonthValue(), now.getYear());
        paymentService.generateMonthlyRentForAllActive(now.getMonthValue(), now.getYear());
    }

    /**
     * Daily at midnight — mark PENDING payments as OVERDUE.
     */
    @Scheduled(cron = "${app.scheduler.overdue-check-cron}")
    public void checkOverduePayments() {
        log.info("[Scheduler] Checking overdue payments");
        paymentService.markOverduePayments();
    }

    /**
     * Daily at 8 AM — expire agreements and send 30-day expiry reminders.
     */
    @Scheduled(cron = "${app.scheduler.agreement-expiry-cron}")
    public void checkAgreementExpiry() {
        log.info("[Scheduler] Checking agreement expiry");
        agreementService.checkAndExpireAgreements();

        // Notify for agreements expiring in the next 30 days
        List<RentalAgreement> expiringSoon = agreementRepository.findExpiringSoon(
            LocalDate.now(), LocalDate.now().plusDays(30)
        );
        expiringSoon.forEach(notificationService::sendAgreementExpiryNotification);

        if (!expiringSoon.isEmpty()) {
            log.info("[Scheduler] Sent expiry reminders for {} agreements", expiringSoon.size());
        }
    }

    /**
     * Every 30 minutes — flush any unsent notifications.
     */
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void flushUnsentNotifications() {
        notificationService.flushUnsent();
    }
}
