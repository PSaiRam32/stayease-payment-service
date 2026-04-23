package com.stayease.payment_service.service;


import com.stayease.payment_service.dto.RefundRequestDTO;
import com.stayease.payment_service.dto.RefundResponseDTO;

public interface RefundService  {
    RefundResponseDTO initiateRefund(RefundRequestDTO request);
    RefundResponseDTO processRefund(Long refundId);
    void completeRefund(Long refundId);
    RefundResponseDTO getRefundDetails(Long refundId);
}
