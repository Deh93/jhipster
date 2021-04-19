package com.astreinte.web.rest;

import com.astreinte.security.TertioRoles;
import com.astreinte.security.jwt.JWTFilter;
import com.astreinte.security.jwt.TokenProvider;
import com.astreinte.service.UserService;
import com.astreinte.service.dto.UserDTO;
import com.astreinte.web.rest.vm.LoginVM;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller to authenticate users.
 */
@Data
@Slf4j
@RestController
@RequestMapping("/api")
public class UserJWTController {

    private final TokenProvider tokenProvider;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;


    private final ActiveDirectoryLdapAuthenticationProvider adAuthProvider;
    private final UserService userService;
    private final UserDetailsService userDetailsService;

    public UserJWTController(TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder, ActiveDirectoryLdapAuthenticationProvider adAuthProvider, UserService userService, UserDetailsService userDetailsService) throws Exception {
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.adAuthProvider = adAuthProvider;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        adAuthProvider.setSearchFilter("(&(objectClass=user)(userPrincipalName={0}))");
        this.authenticationManagerBuilder.userDetailsService(userDetailsService);
        this.authenticationManagerBuilder.authenticationProvider(adAuthProvider);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {

        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
        if (authentication != null && !this.userService.getUserByLogin(loginVM.getUsername()).isPresent()) {
            LdapUserDetails userDetails = (LdapUserDetails) authentication.getPrincipal();
            final List<TertioRoles> roles = TertioRoles.allRoles.stream()
                .filter(TertioRoles -> userDetails.getAuthorities().contains(TertioRoles.getAuthority()))
                .collect(Collectors.toList());
            if(roles.isEmpty()) {
                log.error("{} filtered roles => {}", "JWT CONTROLLER", roles);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if(roles.contains(TertioRoles.ADMIN)){
                roles.add(TertioRoles.B2B);
                roles.add(TertioRoles.COMPLAIN);
            }
            roles.add(TertioRoles.USER);
            this.userService.createUser(UserDTO.builder()
                .login(userDetails.getUsername())
                .firstName(userDetails.getDn()
                    .split(",")[0]
                    .replace("CN=", ""))
                .authorities(new HashSet<>(roles.stream()
                    .map(TertioRoles::getRole)
                    .collect(Collectors.toList())))
                .build());
        }
        String jwt = tokenProvider.createToken(authentication, rememberMe);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}

