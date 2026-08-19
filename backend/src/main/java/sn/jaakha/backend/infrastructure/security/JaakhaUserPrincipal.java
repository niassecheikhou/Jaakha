package sn.jaakha.backend.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import sn.jaakha.backend.domain.enums.Role;
import sn.jaakha.backend.domain.model.AppUser;

public class JaakhaUserPrincipal {
    private final UUID id;
    private final String fullName;
    private final String email;
    private final String phone;
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public JaakhaUserPrincipal(AppUser user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public UUID id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public Role role() {
        return role;
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return authorities;
    }
}
