package edu.udea.hidrologia.shared.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.udea.hidrologia.shared.firebase.FirebaseProperties;
import edu.udea.hidrologia.shared.firebase.FirebaseTokenVerificationException;
import edu.udea.hidrologia.shared.firebase.FirebaseTokenVerifier;
import edu.udea.hidrologia.shared.firebase.VerifiedFirebaseToken;

@Component
class AdminBearerAuthenticationFilter extends OncePerRequestFilter {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_API_PREFIX = "/api/v1/admin";

    private final ObjectProvider<FirebaseTokenVerifier> tokenVerifierProvider;
    private final FirebaseProperties firebaseProperties;

    AdminBearerAuthenticationFilter(
            ObjectProvider<FirebaseTokenVerifier> tokenVerifierProvider,
            FirebaseProperties firebaseProperties) {
        this.tokenVerifierProvider = tokenVerifierProvider;
        this.firebaseProperties = firebaseProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isAdminRequest(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            rejectUnauthorized(request, response);
            return;
        }

        String idToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (idToken.isBlank()) {
            rejectUnauthorized(request, response);
            return;
        }

        Optional<FirebaseTokenVerifier> tokenVerifier = tokenVerifierProvider.stream().findFirst();
        if (tokenVerifier.isEmpty()) {
            rejectUnauthorized(request, response);
            return;
        }

        try {
            VerifiedFirebaseToken verifiedToken = tokenVerifier.get().verify(idToken);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    verifiedToken.uid(),
                    null,
                    authoritiesFor(verifiedToken));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (FirebaseTokenVerificationException exception) {
            SecurityContextHolder.clearContext();
            rejectUnauthorized(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.equals(ADMIN_API_PREFIX) || path.startsWith(ADMIN_API_PREFIX + "/");
    }

    private List<SimpleGrantedAuthority> authoritiesFor(VerifiedFirebaseToken verifiedToken) {
        if (firebaseProperties.getAdminUid().equals(verifiedToken.uid())) {
            return List.of(new SimpleGrantedAuthority(ADMIN_ROLE));
        }

        return List.of();
    }

    private void rejectUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonAuthenticationEntryPoint.writeError(response, request, HttpStatus.UNAUTHORIZED, "Invalid bearer token");
    }
}
