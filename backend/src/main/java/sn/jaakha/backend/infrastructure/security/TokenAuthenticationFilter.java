package sn.jaakha.backend.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sn.jaakha.backend.domain.repository.SessionTokenRepository;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final SessionTokenRepository sessionTokenRepository;

    public TokenAuthenticationFilter(SessionTokenRepository sessionTokenRepository) {
        this.sessionTokenRepository = sessionTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            sessionTokenRepository.findByTokenAndExpiresAtAfter(token, Instant.now())
                    .filter(session -> session.getUser().isActive())
                    .ifPresent(session -> {
                        JaakhaUserPrincipal principal = new JaakhaUserPrincipal(session.getUser());
                        var authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.authorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }
}
