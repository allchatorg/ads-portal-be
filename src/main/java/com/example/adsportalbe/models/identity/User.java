package com.example.adsportalbe.models.identity;

import com.example.adsportalbe.enums.Role;
import com.example.adsportalbe.models.Base;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
// Rename table to avoid using reserved keyword "user" in PostgreSQL
@Table(name = "users")
public class User extends Base implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    @Builder.Default
    private boolean subscribedToMarketingEmails = true;

    @Column(nullable = false)
    private boolean isOver18;

    @Column(nullable = false)
    private boolean acceptsPolicies;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Formula("(select count(a.id) from ads a where a.owner_id = id)")
    private Long purchasedAdsCount;

    @Formula("(select coalesce(sum(a.total_cost), 0) from ads a where a.owner_id = id)")
    private Double totalSpent;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_" + role.name());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
