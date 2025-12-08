package com.example.adsportalbe.models.ad;

import com.example.adsportalbe.enums.AdStatus;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.models.payment.PaymentReceipt;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(optional = false)
    private User owner;

    // A single ad may use multiple formats (photo + text for example)
    @ManyToMany
    @JoinTable(name = "ad_selected_formats", joinColumns = @JoinColumn(name = "ad_id"), inverseJoinColumns = @JoinColumn(name = "format_id"))
    private List<AdFormatSnapshot> selectedFormats; // snapshot, not the live AdFormat

    private String imageUrl;
    private String videoUrl;

    @Column(columnDefinition = "TEXT")
    private String textContent;

    private Integer totalViewsBought;

    private Integer servedViews;

    private Double totalCost;

    @Enumerated(EnumType.STRING)
    private AdStatus status;

    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;

    @OneToOne(mappedBy = "ad", cascade = CascadeType.ALL)
    private PaymentReceipt receipt;
}
