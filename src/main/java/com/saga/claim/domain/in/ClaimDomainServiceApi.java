package com.saga.claim.domain.in;

import com.saga.claim.domain.model.Refund;
import com.saga.claim.domain.model.Claim;
import com.saga.claim.domain.model.enums.ShipmentStatusDomain;

import java.util.List;

public interface ClaimDomainServiceApi {

    void createClaim(String orderId, Integer itemId, Integer merchantInventoryId);

    void createShipment(Integer claimId);

    void assignShipmentToClaim(Claim claim);

    void updateClaimByShipmentStatus(Integer claimId, ShipmentStatusDomain shipmentStatus);

    List<Claim> getClaims();

    void initiateRefund(Refund refund);
}
