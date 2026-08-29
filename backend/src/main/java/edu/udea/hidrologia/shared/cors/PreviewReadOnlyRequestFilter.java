package edu.udea.hidrologia.shared.cors;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class PreviewReadOnlyRequestFilter extends OncePerRequestFilter {

    private static final Set<String> READ_ONLY_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name());

    private final CorsProperties corsProperties;

    PreviewReadOnlyRequestFilter(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);

        if (corsProperties.isPreviewOrigin(origin) && !READ_ONLY_METHODS.contains(request.getMethod())) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
