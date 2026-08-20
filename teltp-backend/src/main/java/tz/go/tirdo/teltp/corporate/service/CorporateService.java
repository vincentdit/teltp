package tz.go.tirdo.teltp.corporate.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.common.Money;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.ReferenceNumberGenerator;
import tz.go.tirdo.teltp.common.exception.BusinessRuleException;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.corporate.dto.CorporateDtos.*;
import tz.go.tirdo.teltp.corporate.entity.ContractStatus;
import tz.go.tirdo.teltp.corporate.entity.TrainingContract;
import tz.go.tirdo.teltp.corporate.repository.TrainingContractRepository;
import tz.go.tirdo.teltp.organization.service.OrganizationService;

@Service
public class CorporateService {

    private final TrainingContractRepository contracts;
    private final OrganizationService organizations;
    private final ReferenceNumberGenerator refGen;

    public CorporateService(TrainingContractRepository contracts, OrganizationService organizations,
                            ReferenceNumberGenerator refGen) {
        this.contracts = contracts;
        this.organizations = organizations;
        this.refGen = refGen;
    }

    @Transactional
    public ContractResponse create(CreateContractRequest req) {
        organizations.getEntity(req.organizationUuid()); // validate org exists
        TrainingContract c = new TrainingContract();
        c.setReferenceNumber(refGen.next("CON"));
        c.setOrganizationUuid(req.organizationUuid());
        c.setTitle(req.title());
        c.setScope(req.scope());
        c.setParticipantTarget(req.participantTarget());
        c.setStartDate(req.startDate());
        c.setEndDate(req.endDate());
        return toResponse(contracts.save(c));
    }

    @Transactional
    public ContractResponse quote(String uuid, QuoteRequest req) {
        TrainingContract c = require(uuid);
        transition(c, ContractStatus.QUOTED);
        c.setContractValue(Money.tzs(req.contractValue()));
        return toResponse(contracts.save(c));
    }

    @Transactional
    public ContractResponse transition(String uuid, TransitionRequest req) {
        TrainingContract c = require(uuid);
        transition(c, ContractStatus.valueOf(req.targetStatus()));
        return toResponse(contracts.save(c));
    }

    @Transactional(readOnly = true)
    public PageResponse<ContractResponse> forOrganization(String organizationUuid, Pageable pageable) {
        return PageResponse.from(contracts.findByOrganizationUuid(organizationUuid, pageable), this::toResponse);
    }

    /** Cross-module hook for Billing to attach an invoice uuid. */
    @Transactional
    public void attachInvoice(String contractUuid, String invoiceUuid) {
        TrainingContract c = require(contractUuid);
        c.setInvoiceUuid(invoiceUuid);
        contracts.save(c);
    }

    public TrainingContract getEntity(String uuid) { return require(uuid); }

    private void transition(TrainingContract c, ContractStatus target) {
        if (!c.getStatus().canTransitionTo(target))
            throw new BusinessRuleException("Illegal contract transition: " + c.getStatus() + " -> " + target);
        c.setStatus(target);
    }

    private TrainingContract require(String uuid) {
        return contracts.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("TrainingContract", uuid));
    }

    private ContractResponse toResponse(TrainingContract c) {
        return new ContractResponse(c.getUuid(), c.getReferenceNumber(), c.getOrganizationUuid(),
                c.getTitle(), c.getScope(), c.getStatus().name(),
                c.getContractValue() == null ? null : c.getContractValue().amount(),
                c.getParticipantTarget(), c.getStartDate(), c.getEndDate(), c.getInvoiceUuid());
    }
}
