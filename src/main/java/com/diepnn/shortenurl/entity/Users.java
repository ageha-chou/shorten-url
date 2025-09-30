package com.diepnn.shortenurl.entity;

import com.diepnn.shortenurl.common.enums.UsersStatus;
import com.diepnn.shortenurl.converter.UsersStatusConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a user in the system.
 */
@Entity
@Table(name = "users",
       indexes = {
               @Index(name = "uidx_users_username", columnList = "username", unique = true),
               @Index(name = "uidx_users_email", columnList = "email", unique = true)
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String email;

    @Column
    private String avatar;

    @Column
    private String username;

    @Column
    @JsonIgnore
    private String password;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    @Convert(converter = UsersStatusConverter.class)
    private UsersStatus status;

    @Column
    private LocalDateTime createdDatetime;

    @Column
    @JsonIgnore
    private LocalDateTime updatedDatetime;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", referencedColumnName = "id",
                foreignKey = @ForeignKey(name = "fk_auth_provider_users"))
    @JsonIgnore
    private List<AuthProvider> authProviders;

    public boolean isActive() {
        return status == UsersStatus.ACTIVE;
    }
}
