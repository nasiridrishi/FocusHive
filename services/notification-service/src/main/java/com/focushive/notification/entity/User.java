package com.focushive.notification.entity;

import com.focushive.notification.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * User entity for test purposes and user identification in the notification service.
 * This is a simple representation for the notification service context.
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    /**
     * Username for the user
     */
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * Email address for the user
     */
    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    /**
     * First name of the user
     */
    @Column(name = "first_name", length = 50)
    private String firstName;

    /**
     * Last name of the user
     */
    @Column(name = "last_name", length = 50)
    private String lastName;

    /**
     * Whether the user is active
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}