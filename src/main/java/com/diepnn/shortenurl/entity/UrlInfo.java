package com.diepnn.shortenurl.entity;

import com.diepnn.shortenurl.common.enums.UrlInfoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UrlInfo {
    @Id
    private Long id;

    @Column
    private String shortUrl;

    @Column
    @Enumerated(EnumType.STRING)
    private UrlInfoStatus status;

    @Column
    private String originalUrl;

    @Column(name = "is_alias")
    private Boolean alias;

    @Column
    private LocalDateTime createdDatetime;

    @Column
    private LocalDateTime lastAccessDatetime;
}
