package com.diepnn.shortenurl.helper;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base class for controller tests to exclude security filter chain.
 */
public abstract class BaseControllerTest {
    @MockitoBean( name = "oauth2SecurityFilterChain")
    protected SecurityFilterChain securityFilterChain;
}
