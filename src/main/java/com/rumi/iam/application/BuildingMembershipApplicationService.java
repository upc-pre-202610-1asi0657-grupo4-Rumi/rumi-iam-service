package com.rumi.iam.application;

import com.rumi.iam.domain.model.Role;
import com.rumi.iam.domain.model.User;
import com.rumi.iam.domain.repository.ProcessedEventRepository;
import com.rumi.iam.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Links users to their buildings from the events of Building Management. Idempotent twice over: an eventId that
 * was already applied is discarded, and linking a building the user already has changes nothing.
 */
@Service
public class BuildingMembershipApplicationService {

    static final String BUILDING_REGISTERED = "BuildingRegistered";

    private final UserRepository userRepository;
    private final ProcessedEventRepository processedEvents;
    private final Clock clock;

    public BuildingMembershipApplicationService(UserRepository userRepository,
                                                ProcessedEventRepository processedEvents, Clock clock) {
        this.userRepository = userRepository;
        this.processedEvents = processedEvents;
        this.clock = clock;
    }

    /**
     * Applies BuildingRegistered: the administrator who registered the building is linked to it.
     *
     * @return false if the event had already been applied
     * @throws MembershipRejectedException if the user does not exist or is not an administrator
     */
    @Transactional
    public boolean linkAdministrator(UUID eventId, UUID buildingId, UUID administratorId) {
        if (processedEvents.isProcessed(eventId)) {
            return false;
        }
        User administrator = userRepository.findById(administratorId)
                .orElseThrow(() -> new MembershipRejectedException("User " + administratorId + " does not exist"));
        if (administrator.getRole() != Role.ADMINISTRATOR) {
            throw new MembershipRejectedException("User " + administratorId + " is not an administrator");
        }
        if (!administrator.belongsTo(buildingId)) {
            userRepository.save(administrator.linkBuilding(buildingId));
        }
        processedEvents.markProcessed(eventId, BUILDING_REGISTERED, clock.instant());
        return true;
    }
}
