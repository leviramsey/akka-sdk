package com.example;

import akka.platform.javasdk.annotations.Acl;
import akka.platform.javasdk.annotations.KalixService;

// tag::main[]
@KalixService
// end::main[]
// Allow all other Kalix services deployed in the same project to access the components of this
// Kalix service, but disallow access from the internet. This can be overridden explicitly
// per component or method using annotations.
// Documentation at https://docs.kalix.io/java/access-control.html
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
// tag::main[]
public class Main { }
// end::main[]