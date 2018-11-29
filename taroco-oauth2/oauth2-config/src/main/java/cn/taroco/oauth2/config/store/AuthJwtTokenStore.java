package cn.taroco.oauth2.config.store;

import cn.taroco.common.constants.CommonConstant;
import cn.taroco.common.constants.SecurityConstants;
import cn.taroco.oauth2.config.util.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bootstrap.encrypt.KeyProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import javax.annotation.Resource;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务器使用 JWT RSA 非对称加密令牌
 *
 * @author liuht
 * @date 2018/7/24 16:21
 */
public class AuthJwtTokenStore {

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean("keyProp")
    public KeyProperties keyProperties() {
        return new KeyProperties();
    }

    @Resource(name = "keyProp")
    private KeyProperties keyProperties;

    @Bean
    public TokenStore tokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) {
        return new JwtTokenStore(jwtAccessTokenConverter);
    }

    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        final JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        KeyPair keyPair = new KeyStoreKeyFactory
                (keyProperties.getKeyStore().getLocation(), keyProperties.getKeyStore().getSecret().toCharArray())
                .getKeyPair(keyProperties.getKeyStore().getAlias());
        converter.setKeyPair(keyPair);
        return converter;
    }

    /**
     * jwt 生成token 定制化处理
     *
     * 添加一些额外的用户信息到token里面
     *
     * @return TokenEnhancer
     */
    @Bean
    public TokenEnhancer tokenEnhancer() {
        return (accessToken, authentication) -> {
            final Map<String, Object> additionalInfo = new HashMap<>(2);
            additionalInfo.put("license", SecurityConstants.LICENSE);
            final Object principal = authentication.getUserAuthentication().getPrincipal();
            UserDetailsImpl user;
            if (principal instanceof UserDetailsImpl) {
                user = (UserDetailsImpl) principal;
            } else {
                final String username = (String) principal;
                user = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
            }
            additionalInfo.put("userId", user.getUserId());
            additionalInfo.put(CommonConstant.HEADER_LABEL, user.getLabel());
            ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
            return accessToken;
        };
    }

}
