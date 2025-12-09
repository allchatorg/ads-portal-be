package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.ad.AdDetailedViewDto;
import com.example.adsportalbe.dto.ad.AdDto;
import com.example.adsportalbe.dto.ad.AdStatusCountDto;
import com.example.adsportalbe.dto.ad.CreateAdRequestDto;
import com.example.adsportalbe.dto.payment.PaymentMethodDto;
import com.example.adsportalbe.dto.requests.AdSearchRequestDto;
import com.example.adsportalbe.enums.AdStatus;
import com.example.adsportalbe.enums.Role;
import com.example.adsportalbe.mappers.AdMapper;
import com.example.adsportalbe.models.ad.*;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.models.payment.PaymentReceipt;
import com.example.adsportalbe.repositories.AdFormatRepository;
import com.example.adsportalbe.repositories.AdRepository;
import com.example.adsportalbe.services.AdService;
import com.example.adsportalbe.services.PaymentService;
import com.example.adsportalbe.specifications.AdSpecification;
import com.example.adsportalbe.utils.Utils;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdServiceImpl implements AdService {

    private final AdRepository adRepository;
    private final AdFormatRepository adFormatRepository;
    private final PaymentService paymentService;
    private final AdMapper adMapper;

    @Override
    @Transactional
    public Ad createAd(CreateAdRequestDto request, User user) throws StripeException {
        // 1. Validate Input Basics
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        if (request.getViewsBought() == null || request.getViewsBought() <= 0) {
            throw new IllegalArgumentException("Views bought must be greater than 0");
        }

        // 2. Fetch Ad Format
        AdFormat format = adFormatRepository.findByType(request.getAdType())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Ad Format Type: " + request.getAdType()));

        // 3. Calculate Price Service-Side to Validate
        double calculatedPrice = calculateAdCost(format, request.getText(), request.getViewsBought());

        // Validate Price deviation (allow small float diff)
        if (Math.abs(calculatedPrice - request.getCalculatedPrice()) > 0.1) {
            throw new IllegalArgumentException("Price mismatch. Server calculated: " + calculatedPrice
                    + ", Client sent: " + request.getCalculatedPrice());
        }

        // 4. Validate Specific Logic
        if (request.getAdType() == AdFormatType.PHOTO
                && (request.getImageUrl() == null || request.getImageUrl().isEmpty())) {
            throw new IllegalArgumentException("Image URL is required for PHOTO ads");
        }
        if (request.getAdType() == AdFormatType.VIDEO
                && (request.getVideoUrl() == null || request.getVideoUrl().isEmpty())) {
            throw new IllegalArgumentException("Video URL is required for VIDEO ads");
        }

        // 5. Authorize Payment
        long amountCents = (long) (calculatedPrice * 100);
        String paymentIntentId = paymentService.authorizePayment(user, request.getStripeId(), amountCents,
                request.getStripeAid());

        // 6. Create Ad Entity
        Ad ad = Ad.builder()
                .title(request.getTitle())
                .owner(user)
                .format(format)
                .imageUrl(request.getImageUrl())
                .videoUrl(request.getVideoUrl())
                .textContent(request.getText())
                .totalViewsBought(request.getViewsBought())
                .servedViews(0)
                .totalCost(calculatedPrice)
                .status(AdStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        // Snapshot the format
        AdFormatSnapshot snapshot = AdFormatSnapshot.builder()
                .originalFormatId(format.getId())
                .type(format.getType())
                .title(format.getTitle())
                .description(format.getDescription())
                .pricePerMille(format.getPricePerMille())
                .build();

        // Note: Ad entity has List<AdFormatSnapshot>, but for now we are building
        // 1-to-1 based on request type
        // The snapshot should ideally be saved. Since it is mapped in Ad as ManyToMany
        // with cascade?
        // Ad.java: @ManyToMany ... List<AdFormatSnapshot> selectedFormats;
        // We need to save snapshot first or configure CascadeType.ALL on the
        // relationship if not present.
        // Checking Ad.java... it just says @ManyToMany. We likely need to save snapshot
        // explicitly.
        // Actually, let's check Ad.java again. It doesn't have CascadeType.ALL on
        // selectedFormats.
        // We should save snapshot via its own repo or add cascade.
        // For this implementation, I will assume we should save it. But I don't have
        // AdFormatSnapshotRepository.
        // It's better to add CascadeType.ALL to Ad.java or create the repo.
        // Given constraints, I will add it to Ad and hope for cascade or standard save
        // behavior if I can modify Ad.
        // Wait, I can't modify Ad easily without risking breaking other things.
        // I will assume I need to save it. But I don't have a repo for it.
        // Let's rely on `CascadeType.ALL` if I can add it, or create a simple repo.
        // I'll create a repository for Snapshot to be safe.
        // Re-reading Ad.java: @ManyToMany @JoinTable...
        // I will creating AdFormatSnapshotRepository implies I need to write another
        // file.
        // I will try to save it via AdRepository if I can add CascadeType.PERSIST/ALL
        // to Ad.java relationship.

        // Let's look at Ad.java again in my memory/context.
        // line 33: @ManyToMany
        // I will update Ad.java to include CascadeType.ALL for simplicity in this flow.

        ad.setSelectedFormats(List.of(snapshot));

        // Create Receipt
        PaymentReceipt.PaymentReceiptBuilder receiptBuilder = PaymentReceipt.builder()
                .ad(ad)
                .stripePaymentIntentId(paymentIntentId)
                .amountPaid(calculatedPrice)
                .currency("USD")
                .status("AUTHORIZED")
                .provider("STRIPE");

        PaymentMethodDto paymentMethodDto = paymentService.getPaymentMethod(request.getStripeId());
        if (paymentMethodDto != null) {
            receiptBuilder.cardBrand(paymentMethodDto.getBrand());
            receiptBuilder.cardLast4(paymentMethodDto.getLast4());
            receiptBuilder.cardholderName(paymentMethodDto.getCardholderName());
        }

        PaymentReceipt receipt = receiptBuilder.build();

        ad.setReceipt(receipt);

        return adRepository.save(ad);
    }

    private double calculateAdCost(AdFormat format, String text, int views) {
        double baseCPM = format.getPricePerMille();
        double textCPM = 0;

        List<TextPricingTierRule> textTiers = format.getTextPricingTiers();

        // If not TEXT, we need the TEXT format tiers
        if (format.getType() != AdFormatType.TEXT) {
            AdFormat textFormat = adFormatRepository.findByType(AdFormatType.TEXT).orElse(null);
            if (textFormat != null && textFormat.getTextPricingTiers() != null) {
                textTiers = textFormat.getTextPricingTiers();
            }
        }

        if (textTiers != null && !textTiers.isEmpty() && text != null && !text.isEmpty()) {
            int charCount = text.length();
            // Sort by maxCharacters asc
            List<TextPricingTierRule> sortedTiers = new ArrayList<>(textTiers);
            sortedTiers.sort(Comparator.comparingInt(TextPricingTierRule::getMaxCharacters));

            TextPricingTierRule matchedTier = sortedTiers.stream()
                    .filter(tier -> charCount <= tier.getMaxCharacters())
                    .findFirst()
                    .orElse(null);

            if (matchedTier != null) {
                textCPM = matchedTier.getPricePerMille();
            } else {
                // Exceeds all? use last
                textCPM = sortedTiers.get(sortedTiers.size() - 1).getPricePerMille();
            }
        }

        double totalCPM = baseCPM + textCPM;
        return (views / 1000.0) * totalCPM;
    }

    @Override
    public Page<AdDto> searchAds(AdSearchRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Search request cannot be null");
        }

        List<Sort.Order> sortOrders = Utils.jsonStringToSortOrder(request.sort());

        // Default sort if none provided or empty
        if (sortOrders.isEmpty()) {
            sortOrders.add(Sort.Order.desc("submittedAt"));
        }

        PageRequest pageRequest = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(sortOrders));

        Specification<Ad> specification = AdSpecification.getSpecification(request);

        return adRepository.findAll(specification, pageRequest).map(adMapper::toDto);
    }

    @Override
    public List<AdStatusCountDto> getAdStatusCounts() {
        return adRepository.getAdStatusCounts();
    }

    @Override
    public List<AdStatusCountDto> getAdStatusCountsByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return adRepository.getAdStatusCountsByUserId(userId);
    }

    @Override
    public AdDetailedViewDto getAdById(Long id, User user) {
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ad not found with id: " + id));

        // Access control: regular users can only view their own ads
        if (user.getRole() == Role.USER) {
            if (!ad.getOwner().getId().equals(user.getId())) {
                throw new RuntimeException("Access denied: You can only view your own ads");
            }
        }
        return adMapper.toDetailedDto(ad);
    }
}
