package tz.go.tirdo.teltp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Strongly-typed binding for the teltp.* configuration tree. */
@Component
@ConfigurationProperties(prefix = "teltp")
public class TeltpProperties {

    private Security security = new Security();
    private Certificate certificate = new Certificate();

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    public Certificate getCertificate() { return certificate; }
    public void setCertificate(Certificate certificate) { this.certificate = certificate; }

    public static class Security {
        private Jwt jwt = new Jwt();
        public Jwt getJwt() { return jwt; }
        public void setJwt(Jwt jwt) { this.jwt = jwt; }
    }

    public static class Jwt {
        private String secret;
        private long accessTokenValiditySeconds = 3600;
        private long refreshTokenValiditySeconds = 2592000;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessTokenValiditySeconds() { return accessTokenValiditySeconds; }
        public void setAccessTokenValiditySeconds(long v) { this.accessTokenValiditySeconds = v; }
        public long getRefreshTokenValiditySeconds() { return refreshTokenValiditySeconds; }
        public void setRefreshTokenValiditySeconds(long v) { this.refreshTokenValiditySeconds = v; }
    }

    public static class Certificate {
        private String verificationBaseUrl = "https://teltp.tirdo.go.tz/verify";
        public String getVerificationBaseUrl() { return verificationBaseUrl; }
        public void setVerificationBaseUrl(String v) { this.verificationBaseUrl = v; }
    }
}
