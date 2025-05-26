package br.com.sisgesmv.secutiry;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.sisgesmv.model.Usuario;
import br.com.sisgesmv.repository.UsuarioRepository;
import br.com.sisgesmv.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UsuarioRepository usuarioRepository;
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    public JwtFilter(JwtTokenService jwtTokenService, UsuarioRepository usuarioRepository) {
        this.jwtTokenService = jwtTokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestPath = request.getServletPath();
        logger.debug("🔍 Verificando requisição: {}", requestPath);

        // Rotas públicas
        if (isPublicRoute(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = getTokenFromRequest(request);
            
            if (token == null) {
                handleMissingToken(response, requestPath);
                return;
            }

            Claims claims = validateTokenAndGetClaims(token, response);
            if (claims == null) return; // Token inválido já foi tratado

            String email = claims.getSubject();
            Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new DisabledException("Usuário não encontrado"));
           

            authenticateUser(request, claims);
            chain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            handleExpiredToken(response, ex);
        } catch (Exception ex) {
            handleGenericError(response, ex);
        }
    }

    private boolean isPublicRoute(String path) {
        return path.startsWith("/auth/") ||
               path.equals("/usuarios/cadastrar") ||
               path.equals("/vendedores/cadastrar") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/webjars/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui");
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. Verificar cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    logger.debug("🔵 Token encontrado no cookie");
                    return cookie.getValue();
                }
            }
        }
        
        // 2. Verificar header Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }

    private Claims validateTokenAndGetClaims(String token, HttpServletResponse response) throws IOException {
        try {
            if (!jwtTokenService.validarToken(token)) {
                logger.warn("⚠️ Token inválido");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                return null;
            }
            return jwtTokenService.extrairClaims(token);
        } catch (ExpiredJwtException ex) {
            throw ex; // Será tratado no bloco catch externo
        } catch (Exception ex) {
            logger.error("❌ Erro ao validar token", ex);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erro ao validar token");
            return null;
        }
    }

    private void authenticateUser(HttpServletRequest request, Claims claims) {
        String email = claims.getSubject();
        String role = claims.get("role", String.class);

        UserDetails userDetails = User.withUsername(email)
                .password("") // Senha não é necessária pois já validamos o token
                .roles(role)
                .build();

        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                userDetails, 
                null, 
                userDetails.getAuthorities());

        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.info("🔑 Usuário autenticado: {}", email);
    }

    private void handleMissingToken(HttpServletResponse response, String requestPath) throws IOException {
        if (!requestPath.equals("/auth/login")) {
            logger.warn("🔴 Token não encontrado para rota protegida: {}", requestPath);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token não fornecido");
        } else {
            logger.debug("🟡 Rota de login - token não requerido");
        }
    }

    private void handleInactiveUser(HttpServletResponse response, String email) throws IOException {
        logger.warn("🚫 Tentativa de acesso de usuário desativado: {}", email);
        invalidarCookieToken(response);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuário desativado");
    }

    private void handleExpiredToken(HttpServletResponse response, ExpiredJwtException ex) throws IOException {
        logger.warn("⏳ Token expirado para usuário: {}", ex.getClaims().getSubject());
        invalidarCookieToken(response);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sessão expirada");
    }

    private void handleGenericError(HttpServletResponse response, Exception ex) throws IOException {
        logger.error("❗ Erro durante autenticação", ex);
        invalidarCookieToken(response);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erro de autenticação");
    }

    private void invalidarCookieToken(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        logger.debug("🧹 Cookie de token invalidado");
    }
}