package com.diepnn.shortenurl.entity;

import com.diepnn.shortenurl.common.enums.ProviderType;
import com.diepnn.shortenurl.converter.ProviderTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a user's authentication provider.
 */
@Entity
@Table(name = "auth_provider",
       indexes = {
               @Index(name = "uidx_auth_provider_provider_id_provider_user_id",
                      columnList = "provider_user_id, provider_id",
                      unique = true)
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The authentication provider (Google, Facebook, etc.)
     */
    @Column(nullable = false)
    @Convert(converter = ProviderTypeConverter.class)
    private ProviderType providerType;

    /**
     * The user's unique identifier within the provider.
     */
    @Column
    private String providerUserId;

    @Column
    private LocalDateTime createdDatetime;

    @Column
    private LocalDateTime lastAccessDatetime;
}
