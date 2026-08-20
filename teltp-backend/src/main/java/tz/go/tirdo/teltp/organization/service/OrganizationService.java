package tz.go.tirdo.teltp.organization.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.common.PageResponse;
import tz.go.tirdo.teltp.common.exception.ResourceNotFoundException;
import tz.go.tirdo.teltp.organization.dto.OrganizationDtos.*;
import tz.go.tirdo.teltp.organization.entity.Organization;
import tz.go.tirdo.teltp.organization.entity.OrganizationSubType;
import tz.go.tirdo.teltp.organization.entity.OrganizationType;
import tz.go.tirdo.teltp.organization.repository.OrganizationRepository;

@Service
public class OrganizationService {

    private final OrganizationRepository repo;

    public OrganizationService(OrganizationRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public OrganizationResponse create(CreateRequest req) {
        Organization org = new Organization();
        org.setName(req.name());
        org.setType(OrganizationType.valueOf(req.type()));
        if (req.subType() != null) org.setSubType(OrganizationSubType.valueOf(req.subType()));
        org.setContactEmail(req.contactEmail());
        org.setContactPhone(req.contactPhone());
        org.setRegion(req.region());
        org.setDistrict(req.district());
        org.setTin(req.tin());
        return toResponse(repo.save(org));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> list(Pageable pageable) {
        return PageResponse.from(repo.findAll(pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(String uuid) {
        return toResponse(require(uuid));
    }

    /** Cross-module resolution hook. */
    public Organization getEntity(String uuid) {
        return require(uuid);
    }

    private Organization require(String uuid) {
        return repo.findByUuid(uuid).orElseThrow(() -> new ResourceNotFoundException("Organization", uuid));
    }

    private OrganizationResponse toResponse(Organization o) {
        return new OrganizationResponse(o.getUuid(), o.getName(), o.getType().name(),
                o.getSubType() == null ? null : o.getSubType().name(),
                o.getContactEmail(), o.getContactPhone(), o.getRegion(), o.getDistrict(), o.getTin());
    }
}
