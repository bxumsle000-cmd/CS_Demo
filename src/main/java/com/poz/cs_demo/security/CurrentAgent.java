package com.poz.cs_demo.security;

import org.springframework.stereotype.Component;

@Component
public class CurrentAgent {
    private static final String DEV_CURRENT_AGENT_ID = "CSC00001" ;
    private static final String DEV_CURRENT_TOKEN = "DEV_CURRENT_TOKEN" ;

    public String currentAgentId() { return DEV_CURRENT_AGENT_ID ; } ;
    public String currentToken() { return DEV_CURRENT_TOKEN ; } ;

}
