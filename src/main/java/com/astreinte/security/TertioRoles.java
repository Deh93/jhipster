package com.astreinte.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
public enum TertioRoles {

    TEST("G_TERTIO_ACCESS", "ROLE_B2B"),
    ADMIN("G_TERTIO_ADMIN", "ROLE_ADMIN"),
    B2B("G_TERTIO_B2B", "ROLE_B2B"),
    COMPLAIN("G_TERTIO_COMPLAIN", "ROLE_COMPLAIN"),
    AGENT("G_TERTIO_AGENT", "ROLE_AGENT"),
    CALLCENTER("G_TERTIO_CALL_CENTER", "ROLE_CALL_CENTER"),
    USER("Itp_CustomerAdvisor", "ROLE_USER");

    public static List<TertioRoles> allRoles;

    static {
        List<TertioRoles> roles = new ArrayList<>(Arrays.asList(TertioRoles.values()));
        allRoles = Collections.unmodifiableList(roles);
    }

    private String activeDirectoryGroup;
    private String role;
    private GrantedAuthority authority;


    TertioRoles(String activeDirectoryGroup, String role) {
        this.activeDirectoryGroup = activeDirectoryGroup;
        this.role = role;
        this.authority = new SimpleGrantedAuthority(activeDirectoryGroup);
    }
}
