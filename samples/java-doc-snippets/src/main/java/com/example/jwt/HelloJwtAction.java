package com.example.jwt;

import akka.platform.javasdk.timedaction.TimedAction;
import akka.platform.javasdk.annotations.ComponentId;
import akka.platform.javasdk.annotations.JWT;

//TODO migrate to endpoint is needed
// tag::bearer-token[]
@ComponentId("hello-jwt")
@JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN, 
     bearerTokenIssuer = "my-issuer") // <1>
public class HelloJwtAction extends TimedAction {

    public TimedAction.Effect message(String msg) {
        //..
    // end::bearer-token[]    
        return effects().done();
    // tag::bearer-token[]
    }

    @JWT(validate = JWT.JwtMethodMode.BEARER_TOKEN,
         bearerTokenIssuer = "my-other-issuer")     
    public TimedAction.Effect messageWithIssuer(String msg) { // <3>
        //..
    // end::bearer-token[]    
        return effects().done();
    // tag::bearer-token[]        
    }  
}
// end::bearer-token[]
