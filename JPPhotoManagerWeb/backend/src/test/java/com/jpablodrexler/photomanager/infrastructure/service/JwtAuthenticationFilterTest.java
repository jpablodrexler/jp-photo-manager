package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtTokenPort jwtUtil;
    @Mock UserDetailsService userDetailsService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;

    private JwtAuthenticationFilter sut;

    @BeforeEach
    void setUp() {
        sut = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noCookies_continuesChainWithoutAuth() throws Exception {
        when(request.getCookies()).thenReturn(null);

        sut.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_noJwtCookie_continuesChainWithoutAuth() throws Exception {
        Cookie[] cookies = {new Cookie("session", "abc")};
        when(request.getCookies()).thenReturn(cookies);

        sut.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_invalidToken_continuesChainWithoutAuth() throws Exception {
        Cookie[] cookies = {new Cookie("jwt", "bad-token")};
        when(request.getCookies()).thenReturn(cookies);
        when(jwtUtil.isTokenValid("bad-token")).thenReturn(false);

        sut.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        String token = "valid-token";
        Cookie[] cookies = {new Cookie("jwt", token)};
        UserDetails userDetails = new User("alice", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(request.getCookies()).thenReturn(cookies);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

        sut.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
    }

    @Test
    void doFilterInternal_alreadyAuthenticated_skipsUserDetailsLoad() throws Exception {
        String token = "valid-token";
        Cookie[] cookies = {new Cookie("jwt", token)};
        UserDetails userDetails = new User("alice", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(request.getCookies()).thenReturn(cookies);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

        sut.doFilterInternal(request, response, filterChain);
        sut.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, times(1)).loadUserByUsername("alice");
    }
}
