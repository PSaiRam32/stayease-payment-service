package com.stayease.payment_service.service;


import com.stayease.payment_service.dto.Request.RefundRequest;
import com.stayease.payment_service.dto.Response.RefundResponse;

public interface RefundService {
    RefundResponse initiateRefund(RefundRequest request);
    RefundResponse getRefundDetails(Long refundId);
    RefundResponse processRefund(Long refundId);
    void completeRefund(Long refundId);
}
