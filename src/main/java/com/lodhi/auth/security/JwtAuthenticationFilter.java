package com.lodhi.auth.security;

import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.services.UserService;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.type.CollectionLikeType;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor



public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh"
    );

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header=request.getHeader("Authorization");
        if(header !=null && header.startsWith("Bearer ")){
            String token=header.substring(7);
            if(!jwtService.isAccessToken(token)){
                filterChain.doFilter(request,response);
                return;
            }
            try {
                Jws<Claims> parsed = jwtService.parseToken(token);


                Claims claims = parsed.getPayload();



                String id = claims.getId();
                Long userId = Long.parseLong(id);

                userRepository.findById(userId).ifPresent(user -> {

                    List<GrantedAuthority> authorityList =
                            user.getRoles() == null
                                    ? List.of()
                                    : user.getRoles()
                                    .stream()
                                    .map(role ->
                                         new SimpleGrantedAuthority(role.getRoleName())
                                    )
                                    .collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authentication=new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorityList);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    if(SecurityContextHolder.getContext().getAuthentication()==null){
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });

            }catch (ExpiredJwtException e)
            {

            }catch (MalformedJwtException e){

            }catch(JwtException e){

            }catch(Exception e){

            }


        }
        filterChain.doFilter(request,response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_ENDPOINTS.contains(request.getRequestURI());
    }
}
