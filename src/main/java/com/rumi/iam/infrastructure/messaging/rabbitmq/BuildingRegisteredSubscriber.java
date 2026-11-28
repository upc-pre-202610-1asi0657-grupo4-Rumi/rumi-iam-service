package com.rumi.iam.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumi.iam.application.BuildingMembershipApplicationService;
import com.rumi.iam.application.MembershipRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Links the administrator to the building it registered. The body is read here, not by a message converter, so
 * the contract is only the JSON (no Java type headers). A malformed message, an unknown user or a user who is not
 * an administrator go to the dead letter queue; the same event delivered twice is applied once.
 */
@Component
public class BuildingRegisteredSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildingRegisteredSubscriber.class);

    private final BuildingMembershipApplicationService membershipService;
    private final ObjectMapper objectMapper;

    public BuildingRegisteredSubscriber(BuildingMembershipApplicationService membershipService,
                                        ObjectMapper objectMapper) {
        this.membershipService = membershipService;
        this.objectMapper = objectMapper.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @RabbitListener(queues = IamMessagingConfiguration.BUILDING_REGISTERED_QUEUE)
    public void onBuildingRegistered(Message message) {
        BuildingRegisteredMessage event = read(message);
        try {
            boolean applied = membershipService.linkAdministrator(
                    event.eventId(), event.buildingId(), event.administratorId());
            if (applied) {
                LOGGER.info("Linked administrator {} to building {} (event {})",
                        event.administratorId(), event.buildingId(), event.eventId());
            } else {
                LOGGER.info("BuildingRegistered {} was already applied; discarded", event.eventId());
            }
        } catch (MembershipRejectedException rejected) {
            throw new AmqpRejectAndDontRequeueException(
                    "BuildingRegistered " + event.eventId() + " rejected: " + rejected.getMessage(), rejected);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            // Another delivery of the same event committed first; the processed_events key stopped this one.
            LOGGER.info("BuildingRegistered {} was applied by a concurrent delivery; discarded", event.eventId());
        }
    }

    private BuildingRegisteredMessage read(Message message) {
        try {
            BuildingRegisteredMessage event = objectMapper.readValue(message.getBody(), BuildingRegisteredMessage.class);
            if (event == null || !event.isComplete()) {
                throw new AmqpRejectAndDontRequeueException("BuildingRegistered message is missing fields");
            }
            return event;
        } catch (IOException malformed) {
            throw new AmqpRejectAndDontRequeueException("BuildingRegistered message is not valid JSON", malformed);
        }
    }
}
