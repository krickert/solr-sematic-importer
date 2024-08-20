   package com.krickert.search.indexer.config;

   import io.micronaut.context.annotation.ConfigurationProperties;
   import io.micronaut.context.annotation.EachProperty;
   import io.micronaut.context.annotation.Parameter;
   import io.micronaut.serde.annotation.Serdeable;

   @Serdeable
   @EachProperty("solr-config")
   public class SolrConfiguration {
       private String name;
       private String version;
       private String collection;
       private Connection connection;

       public SolrConfiguration(@Parameter("name") String name) {
           this.name = name;
       }

       // Getters and Setters

       public String getName() {
           return name;
       }

       public void setName(String name) {
           this.name = name;
       }

       public String getVersion() {
           return version;
       }

       public void setVersion(String version) {
           this.version = version;
       }

       public String getCollection() {
           return collection;
       }

       public void setCollection(String collection) {
           this.collection = collection;
       }

       public Connection getConnection() {
           return connection;
       }

       public void setConnection(Connection connection) {
           this.connection = connection;
       }

       @Serdeable
       @ConfigurationProperties("connection")
       public static class Connection {
           private String url;
           private Authentication authentication;

           // Getters and Setters
           public String getUrl() {
               return url;
           }

           public void setUrl(String url) {
               this.url = url;
           }

           public Authentication getAuthentication() {
               return authentication;
           }

           public void setAuthentication(Authentication authentication) {
               this.authentication = authentication;
           }

           @Serdeable
           @ConfigurationProperties("authentication")
           public static class Authentication {
               private boolean enabled;
               private String type;
               private String clientSecret;
               private String clientId;
               private String issuer;
               private String issuerAuthId;
               private String subject;

               // Getters and Setters
               public boolean isEnabled() {
                   return enabled;
               }

               public void setEnabled(boolean enabled) {
                   this.enabled = enabled;
               }

               public String getType() {
                   return type;
               }

               public void setType(String type) {
                   this.type = type;
               }

               public String getClientSecret() {
                   return clientSecret;
               }

               public void setClientSecret(String clientSecret) {
                   this.clientSecret = clientSecret;
               }

               public String getClientId() {
                   return clientId;
               }

               public void setClientId(String clientId) {
                   this.clientId = clientId;
               }

               public String getIssuer() {
                   return issuer;
               }

               public void setIssuer(String issuer) {
                   this.issuer = issuer;
               }

               public String getIssuerAuthId() {
                   return issuerAuthId;
               }

               public void setIssuerAuthId(String issuerAuthId) {
                   this.issuerAuthId = issuerAuthId;
               }

               public String getSubject() {
                   return subject;
               }

               public void setSubject(String subject) {
                   this.subject = subject;
               }
           }
       }
   }