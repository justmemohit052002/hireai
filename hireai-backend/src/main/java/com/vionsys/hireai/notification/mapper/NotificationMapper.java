
package com.vionsys.hireai.notification.mapper;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.vionsys.hireai.notification.dto.NotificationResponse;
import com.vionsys.hireai.notification.dto.SendNotificationRequest;
import com.vionsys.hireai.notification.entity.Notification;

        @Component
        public class NotificationMapper {

            public Notification toEntity(
                    @NonNull SendNotificationRequest request) {

                Notification notification = new Notification();

                notification.setCandidateId(
                        request.getCandidateId()
                );

                notification.setJobId(
                        request.getJobId()
                );

                notification.setApplicationId(
                        request.getApplicationId()
                );

                notification.setTitle(
                        request.getTitle()
                );

                notification.setMessage(
                        request.getMessage()
                );

                notification.setType(
                        request.getType()
                );

                notification.setChannel(
                        request.getChannel()
                );

                return notification;
            }

            public NotificationResponse toResponse(
                    @NonNull Notification notification) {

                NotificationResponse response =
                        new NotificationResponse();

                response.setId(
                        notification.getId()
                );

                response.setCandidateId(
                        notification.getCandidateId()
                );

                response.setJobId(
                        notification.getJobId()
                );

                response.setApplicationId(
                        notification.getApplicationId()
                );

                response.setTitle(
                        notification.getTitle()
                );

                response.setMessage(
                        notification.getMessage()
                );

                response.setType(
                        notification.getType()
                );

                response.setChannel(
                        notification.getChannel()
                );

                response.setReadStatus(
                        notification.getReadStatus()
                );

                response.setCreatedAt(
                        notification.getCreatedAt()
                );

                return response;
            }
        }