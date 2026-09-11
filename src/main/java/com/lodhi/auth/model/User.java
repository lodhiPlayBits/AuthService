package com.lodhi.auth.model;

import com.lodhi.auth.enums.Gender;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.model.Role;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder

@Entity
@Table(name = "user_table")


public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="user_id")
    private Long Id;
    @Column(unique = true,nullable = false)
    private String email;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Column(unique = true,nullable = false)
    private String phoneNumber;
    @Column(nullable = false)
    private String password;

    private String image;

    @Column(nullable = false)

    private boolean isEnable;

    @Column(nullable = false)
    private Instant createdAt= Instant.now() ;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider=Provider.LOCAL;

    @Column(nullable = false)
    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(
            name="user_roles",
            joinColumns = @JoinColumn(
                    name="user_id",
                    nullable = false
            ),
            inverseJoinColumns = @JoinColumn(
                    name="role_id",
                    nullable=false
            )

    )
    private Set<Role> roles=new HashSet<>();

    @PrePersist
    protected void onCreate(){
        Instant now=Instant.now();
        if(createdAt==null) createdAt=now;
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt=Instant.now();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .toList();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnable;
    }


}
