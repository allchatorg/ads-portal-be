package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.payment.PaymentMethodDto;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.UserRepository;
import com.example.adsportalbe.services.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodListParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public String createCustomer(User user) throws StripeException {
        if (user.getStripeCustomerId() != null) {
            return user.getStripeCustomerId();
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFirstName() + " " + user.getLastName())
                .build();

        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user); // Persist connection

        return customer.getId();
    }

    @Override
    public List<PaymentMethodDto> getPaymentMethods(User user) throws StripeException {
        String customerId = createCustomer(user); // Ensure customer exists

        PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();

        PaymentMethodCollection paymentMethods = PaymentMethod.list(params);

        List<PaymentMethodDto> dtos = new ArrayList<>();
        for (PaymentMethod pm : paymentMethods.getData()) {
            if (pm.getCard() != null) {
                dtos.add(PaymentMethodDto.builder()
                        .id(pm.getId())
                        .brand(pm.getCard().getBrand())
                        .last4(pm.getCard().getLast4())
                        .expMonth(pm.getCard().getExpMonth())
                        .expYear(pm.getCard().getExpYear())
                        .build());
            }
        }
        return dtos;
    }

    @Override
    public void addPaymentMethod(User user, String paymentMethodId) throws StripeException {
        String customerId = createCustomer(user);

        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

        PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build();

        paymentMethod.attach(params);
    }

    @Override
    public void removePaymentMethod(User user, String paymentMethodId) throws StripeException {
        // First verify this card belongs to the user
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

        // Basic check to ensure we are not deleting someone else's card if the ID is
        // guessed
        // (Stripe IDs are distinct, but good detailed check)
        String customerId = createCustomer(user);
        if (!customerId.equals(paymentMethod.getCustomer())) {
            throw new IllegalArgumentException("Payment method does not belong to the user");
        }

        paymentMethod.detach();
    }
}
