package com.rentsphere.service;

import com.rentsphere.dto.request.PaymentRequest;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.PaymentResponse;
import com.rentsphere.entity.Payment;

public interface PaymentService {
    PaymentResponse generateMonthlyPayment(Long agreementId, int month, int year, Long requesterId);
    PaymentResponse recordPayment(Long paymentId, PaymentRequest request, Long requesterId);
    PaymentResponse findById(Long id);
    PagedResponse<PaymentResponse> findByTenant(Long tenantId, int page, int size);
    PagedResponse<PaymentResponse> findByAgreement(Long agreementId, int page, int size);
    PagedResponse<PaymentResponse> findByOwner(Long ownerId, int page, int size);
    PagedResponse<PaymentResponse> findByStatus(Payment.PaymentStatus status, int page, int size);
    void generateMonthlyRentForAllActive(int month, int year); // scheduler
    void markOverduePayments();                                // scheduler
}
