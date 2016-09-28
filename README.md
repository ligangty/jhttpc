#jHTTPc - A Httpclient Wrapper

jHTTPc is a wrapper around Apache HttpClient that simplifies use cases related to server-side embedding, such as PEM-oriented SSL configuration. Using jHTTPc, you don't have to work directly with KeyStores, and you can specify an independent SSL configuration for each host to which your server connects. You can use PEM files on the system, or even PEM-encoded strings stored by some other mechanism. This also makes it easier to support user-driven configuration of these hosts via a server UI (or even REST), since keystore management happens behind the scenes. 

* [Basics](#basics)
* [Proxies](#proxies)
* [Basic Authentication](#basic_auth)
* [Other Goodies](#etc)
* [Custom Authenticators](#authenticators)

##Basics
<a name="basics"></a>

It's very easy to start using:

```
SiteConfigBuilder siteBuilder = new SiteConfigBuilder( "test", "http://www.somesite.com" )
                  .withKeyCertPem( FileUtils.readFileToString( "/path/to/client.pem" ) )
                  .withServerCertPem( FileUtils.readFileToString( "/path/to/my-server.pem" ) );

SiteConfig site = siteBuilder.build();

MemoryPasswordManager passwords = new MemoryPasswordManager();
passwords.bind( "my K3y password!", site, PasswordType.KEY );

HttpFactory factory = new HttpFactory( passwords );

HttpClient client = factory.createClient( site );
[...]
```

##Proxies
<a name="proxies"></a>

If you need to use a proxy server (with authentication), you can add the following:

```
siteBuilder.withProxyHost( "some.proxy.host" ).withProxyPort( 8080 ).withProxyUser( "someuser" );

SiteConfig site = siteBuilder.build();

passwords.bind( "somepassword", siteConfig, PasswordType.PROXY );
```

##Basic Authentication
<a name="basic_auth"></a>

If you need good old basic authentication (in addition to client SSL?!), you can specify that as well:

```
siteBuilder.withUser( "someuser" );

SiteConfig site = siteBuilder.build();

passwords.bind( "somepassword", siteConfig, PasswordType.USER );
```

##Other Goodies
<a name="etc"></a>

There are other configurations you can specify on a per-site basis as well:

```
siteBuilder.withMaxConnections( 20 )
           .withRequestTimeoutSeconds( 30 )
           .withTrustType( ServerTrustType.TRUST_SELF_SIGNED );
```

##Custom Authenticators
<a name="authenticators"></a>

If you need some other authentication mechanism, you can implement that by extending `ClientAuthenticator` and passing in your own authenticator instance via the `HttpFactory(ClientAuthenticator)` constructor. For instance, a simple OAuth bearer token authenticator might look like this:

```
public class OAuth20BearerTokenAuthenticator
        extends ClientAuthenticator
{

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_FORMAT = "Bearer %s";

    private final String token;

    public OAuth20BearerTokenAuthenticator( final String token )
    {
        this.token = token;
    }

    @Override
    public HttpClientBuilder decorateClientBuilder( final HttpClientBuilder builder )
            throws JHttpCException
    {
        final Header header = new BasicHeader( AUTHORIZATION_HEADER, String.format( BEARER_FORMAT, token ) );
        return builder.setDefaultHeaders( Collections.<Header> singleton( header ) );
    }

}

```

Then, simply construct the `HttpFactory` using the new authenticator:

```
HttpFactory factory = new HttpFactory( new OAuth20BearerTokenAuthenticator( token ) );
```
