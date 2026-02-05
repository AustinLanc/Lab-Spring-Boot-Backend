package org.example.companyboiler.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;

@Service
public class LdapAuthService {

    @Value("${ldap.url}")
    private String ldapUrl;

    @Value("${ldap.base.dn}")
    private String baseDn;

    @Value("${ldap.user.search.base}")
    private String userSearchBase;

    @Value("${ldap.user.search.filter}")
    private String userSearchFilter;

    @Value("${ldap.domain:}")
    private String ldapDomain;

    public LdapUserInfo authenticate(String username, String password) {
        DirContext ctx = null;
        try {
            // For Active Directory, use UPN format (username@domain.com) for authentication
            String userPrincipal;
            if (username.contains("@") || username.contains("\\")) {
                // Already in UPN or down-level format
                userPrincipal = username;
            } else if (ldapDomain != null && !ldapDomain.isEmpty()) {
                // Use configured domain
                userPrincipal = username + "@" + ldapDomain;
            } else {
                // Extract domain from base DN (dc=mgs,dc=com -> mgs.com)
                userPrincipal = username + "@" + extractDomainFromBaseDn();
            }

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, userPrincipal);
            env.put(Context.SECURITY_CREDENTIALS, password);

            ctx = new InitialDirContext(env);

            // If we get here, authentication was successful
            // Try to get user info
            return getUserInfo(ctx, username);

        } catch (NamingException e) {
            System.err.println("LDAP authentication failed for user: " + username + " - " + e.getMessage());
            return null;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    // ignore
                }
            }
        }
    }

    private String findUserDn(String username) {
        DirContext ctx = null;
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapUrl);
            // Anonymous bind or use a service account if required
            env.put(Context.SECURITY_AUTHENTICATION, "none");

            ctx = new InitialDirContext(env);

            String searchFilter = userSearchFilter.replace("{0}", username);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<SearchResult> results = ctx.search(
                    userSearchBase + "," + baseDn,
                    searchFilter,
                    searchControls
            );

            if (results.hasMore()) {
                SearchResult result = results.next();
                return result.getNameInNamespace();
            }
        } catch (NamingException e) {
            // Anonymous bind might not be allowed, that's ok
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    private LdapUserInfo getUserInfo(DirContext ctx, String username) {
        try {
            String searchFilter = userSearchFilter.replace("{0}", username);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"cn", "displayName", "mail", "sAMAccountName", "memberOf"});

            NamingEnumeration<SearchResult> results = ctx.search(
                    baseDn,
                    searchFilter,
                    searchControls
            );

            if (results.hasMore()) {
                SearchResult result = results.next();
                Attributes attrs = result.getAttributes();

                String displayName = getAttributeValue(attrs, "displayName");
                if (displayName == null) {
                    displayName = getAttributeValue(attrs, "cn");
                }
                if (displayName == null) {
                    displayName = username;
                }

                String email = getAttributeValue(attrs, "mail");

                return new LdapUserInfo(username, displayName, email);
            }
        } catch (NamingException e) {
            System.err.println("Error getting user info: " + e.getMessage());
        }

        // Return basic info if we couldn't get details
        return new LdapUserInfo(username, username, null);
    }

    private String getAttributeValue(Attributes attrs, String name) {
        try {
            Attribute attr = attrs.get(name);
            if (attr != null) {
                return (String) attr.get();
            }
        } catch (NamingException e) {
            // ignore
        }
        return null;
    }

    private String extractDomainFromBaseDn() {
        // Convert "dc=mgs,dc=com" to "mgs.com"
        if (baseDn == null || baseDn.isEmpty()) {
            return "";
        }
        StringBuilder domain = new StringBuilder();
        String[] parts = baseDn.split(",");
        for (String part : parts) {
            String trimmed = part.trim().toLowerCase();
            if (trimmed.startsWith("dc=")) {
                if (domain.length() > 0) {
                    domain.append(".");
                }
                domain.append(trimmed.substring(3));
            }
        }
        return domain.toString();
    }

    public record LdapUserInfo(String username, String displayName, String email) {}
}
