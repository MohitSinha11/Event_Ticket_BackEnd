package com.example.tickets.services.impl;

import com.example.tickets.domain.entities.*;
import com.example.tickets.exceptions.QrCodeNotFoundException;
import com.example.tickets.exceptions.TicketNotFoundException;
import com.example.tickets.repositories.QrCodeRepository;
import com.example.tickets.repositories.TicketRepository;
import com.example.tickets.repositories.TicketValidationRepository;
import com.example.tickets.services.TicketValidationService;
import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketValidationServiceImpl implements TicketValidationService {

    private final QrCodeRepository qrCodeRepository;
    private final TicketValidationRepository ticketValidationRepository;
    private final TicketRepository ticketRepository;

    @Override
    public TicketValidation validateTicketByQrCode(UUID qrCodeId) {
        QrCode qrCode = qrCodeRepository.findByIdAndStatus(qrCodeId , QrCodeStatusEnum.ACTIVE)
                .orElseThrow(() -> new QrCodeNotFoundException(
                        String.format("Ticket QR Code with id %s not found", qrCodeId)
                ));

        Ticket ticket = qrCode.getTicket();

        return validateTicket(ticket , TicketValidationMethod.QR_SCAN);
    }

    @Override
    public TicketValidation validateTicketManually(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(TicketNotFoundException::new);

        return validateTicket(ticket , TicketValidationMethod.MANUAL);
    }

    private TicketValidation validateTicket(Ticket ticket , TicketValidationMethod method) {
        TicketValidation ticketValidation = new TicketValidation();
        ticketValidation.setTicket(ticket);
        ticketValidation.setValidationMethod(method);

        TicketValidationStatusEnum ticketValidationStatus = ticket.getValidations().stream()
                .filter(v -> TicketValidationStatusEnum.VALID.equals(v.getStatus()))
                .findFirst()
                .map(v -> TicketValidationStatusEnum.INVALID)
                .orElse(TicketValidationStatusEnum.VALID);

        ticketValidation.setStatus(ticketValidationStatus);
        return ticketValidationRepository.save(ticketValidation);
    }
}
