package com.coffeeshop.crm.dto.response;

import java.util.List;

public record PingResponse(String module, String authenticatedAs, List<String> authorities) { }
