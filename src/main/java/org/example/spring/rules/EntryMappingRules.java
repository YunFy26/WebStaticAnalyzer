package org.example.spring.rules;

import java.util.HashMap;
import java.util.Map;

public class EntryMappingRules {

    public static final String REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";

    public static final String GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";

    public static final String POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping";

    public static final String PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping";

    public static final String DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping";

    public static final String PATCH_MAPPING = "org.springframework.web.bind.annotation.PatchMapping";

    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, ANY
    }

}
