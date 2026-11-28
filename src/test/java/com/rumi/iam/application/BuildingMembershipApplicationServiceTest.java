package com.rumi.iam.application;

import com.rumi.iam.UserFixtures;
import com.rumi.iam.domain.model.EmailAddress;
import com.rumi.iam.domain.model.Role;
import com.rumi.iam.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildingMembershipApplicationServiceTest {

    private static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    private static final UUID OTHER_BUILDING_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
    private final BuildingMembershipApplicationService service = new BuildingMembershipApplicationService(
            users, processedEvents, Clock.fixed(Instant.parse("2026-11-28T06:00:00Z"), ZoneOffset.UTC));

    @Test
    void linksTheAdministratorToTheBuildingItRegistered() {
        users.save(UserFixtures.administrator());
        UUID eventId = UUID.randomUUID();

        assertThat(service.linkAdministrator(eventId, BUILDING_ID, UserFixtures.USER_ID)).isTrue();

        assertThat(users.findById(UserFixtures.USER_ID).orElseThrow().getBuildingIds()).containsExactly(BUILDING_ID);
        assertThat(processedEvents.processed).containsEntry(eventId, "BuildingRegistered");
    }

    @Test
    void anEventDeliveredAgainIsDiscardedByItsEventId() {
        users.save(UserFixtures.administrator());
        UUID eventId = UUID.randomUUID();
        service.linkAdministrator(eventId, BUILDING_ID, UserFixtures.USER_ID);

        // Same eventId: discarded, whatever the payload says.
        assertThat(service.linkAdministrator(eventId, OTHER_BUILDING_ID, UserFixtures.USER_ID)).isFalse();

        assertThat(users.findById(UserFixtures.USER_ID).orElseThrow().getBuildingIds()).containsExactly(BUILDING_ID);
        assertThat(processedEvents.processed).hasSize(1);
    }

    @Test
    void aSecondEventForABuildingAlreadyLinkedChangesNothing() {
        users.save(UserFixtures.administrator().linkBuilding(BUILDING_ID));

        assertThat(service.linkAdministrator(UUID.randomUUID(), BUILDING_ID, UserFixtures.USER_ID)).isTrue();

        assertThat(users.findById(UserFixtures.USER_ID).orElseThrow().getBuildingIds()).containsExactly(BUILDING_ID);
    }

    @Test
    void anUnknownUserIsRejectedAndTheEventIsNotMarkedAsProcessed() {
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> service.linkAdministrator(eventId, BUILDING_ID, UserFixtures.USER_ID))
                .isInstanceOf(MembershipRejectedException.class)
                .hasMessage("User " + UserFixtures.USER_ID + " does not exist");
        assertThat(processedEvents.processed).isEmpty();
    }

    @Test
    void aResidentCannotBeLinkedAsTheAdministratorOfABuilding() {
        UUID residentId = UUID.randomUUID();
        users.save(new User(residentId, new EmailAddress("luis@rumi.pe"), "Luis Quispe", UserFixtures.PASSWORD_HASH,
                Role.RESIDENT, Set.of(), UserFixtures.CREATED_AT));

        assertThatThrownBy(() -> service.linkAdministrator(UUID.randomUUID(), BUILDING_ID, residentId))
                .isInstanceOf(MembershipRejectedException.class)
                .hasMessage("User " + residentId + " is not an administrator");
        assertThat(users.findById(residentId).orElseThrow().getBuildingIds()).isEmpty();
    }
}
