package RIKU.server.Security;

import RIKU.server.Entity.Base.BaseStatus;
import RIKU.server.Entity.User.UserRole;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class AuthMember implements UserDetails {

    private final Long id;

    private final String username;

    private final String password;

    private final List<GrantedAuthority> authorities;

    private final BaseStatus status;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.unmodifiableList(authorities);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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
        return status == BaseStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals(UserRole.ADMIN.getRole()));
    }

    private AuthMember(Long id, String username, String password, List<GrantedAuthority> authorities, BaseStatus status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.status = status;
    }

    public static AuthMember of(Long id, String username, String password, List<GrantedAuthority> authorities, BaseStatus status) {
        return new AuthMember(id, username, password, authorities, status);
    }
}
