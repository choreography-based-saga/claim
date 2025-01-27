package com.saga.claim.domain.service;

import com.saga.claim.domain.model.Refund;
import com.saga.claim.domain.in.ClaimDomainServiceApi;
import com.saga.claim.domain.model.Claim;
import com.saga.claim.domain.model.enums.ClaimStatusDomain;
import com.saga.claim.domain.model.enums.ShipmentStatusDomain;
import com.saga.claim.domain.out.ClaimProducerApi;
import com.saga.claim.domain.out.ClaimRepositoryApi;
import com.saga.claim.domain.out.ShipmentProducerApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class ClaimDomainService implements ClaimDomainServiceApi {

    private final ClaimRepositoryApi claimRepositoryApi;
    private final ShipmentProducerApi shipmentProducerApi;
    private final ClaimProducerApi claimProducerApi;

    @Override
    public void createClaim(String orderId, Integer itemId, Integer merchantInventoryId, UUID customerId, UUID recipientId) {
        claimRepositoryApi.createClaim(orderId, itemId, merchantInventoryId, customerId, recipientId);
    }

    @Override
    public void createShipment(Integer claimId) {
        Optional<Claim> maybeClaim = claimRepositoryApi.getClaimById(claimId);
        if (maybeClaim.isEmpty()) {
            throw new RuntimeException("Invalid claim id: " + claimId);
        }
        Claim claim = maybeClaim.get();
        claim = claim.updateStatus(ClaimStatusDomain.RETURNING_TO_WAREHOUSE);
        claimRepositoryApi.save(claim);
        shipmentProducerApi.createShipment(claim);
        log.info("Initiated shipment for claim: {}", claimId);
    }

    @Override
    public void assignShipmentToClaim(Claim claim) {
        Optional<Claim> maybeClaim = claimRepositoryApi.getClaimById(claim.id());
        if (maybeClaim.isEmpty()) {
            throw new RuntimeException("Couldn't find claim with id: " + "on update claim event");
        }
        Claim claimToUpdate = maybeClaim.get();
        if (claim.status() != null) {
            claimToUpdate = claimToUpdate.updateStatus(claim.status());
        }
        if (claim.shipmentId() != null) {
            claimToUpdate = claimToUpdate.setShipmentId(claim.shipmentId());
        }
        claimRepositoryApi.save(claimToUpdate);
    }

    @Override
    public void updateClaimByShipmentStatus(Integer claimId, ShipmentStatusDomain shipmentStatus) {
        Optional<Claim> maybeClaim = claimRepositoryApi.getClaimById(claimId);
        if (maybeClaim.isEmpty()) {
            throw new RuntimeException("Couldn't find claim with id: " + "on update claim event");
        }
        Claim claimToUpdate = maybeClaim.get();
        if (shipmentStatus.equals(ShipmentStatusDomain.DELIVERED)) {
            claimToUpdate = claimToUpdate.updateStatus(ClaimStatusDomain.DELIVERED);

        }
        claimRepositoryApi.save(claimToUpdate);
        claimProducerApi.sendClaim(claimToUpdate);
    }

    @Override
    public List<Claim> getClaims() {
        return claimRepositoryApi.getAll();
    }

    @Override
    public void initiateRefund(Refund refund) {
        Optional<Claim> maybeClaim = claimRepositoryApi.getClaimById(refund.claimId());
        if (maybeClaim.isEmpty()) {
            throw new RuntimeException("Couldn't find claim with id: " + "on refund request");
        }
        Claim claim = maybeClaim.get();
        claim = claim.updateStatus(ClaimStatusDomain.REFUNDED);
        claim = claim.updateRefundAmount(refund.refundAmount());

        claimRepositoryApi.save(claim);
        claimProducerApi.sendClaim(claim);
    }
}
