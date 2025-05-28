package com.backend.backend.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置类
 * 用于读取配置文件中的参数并初始化AlipayClient
 */
@Configuration
public class AlipayConfig {

    @Value("${alipay.sandbox.app-id}")
    private String appId;

    @Value("${alipay.sandbox.merchant-private-key}")
    private String merchantPrivateKey;

    @Value("${alipay.sandbox.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${alipay.sandbox.gateway-url}")
    private String gatewayUrl;

    @Value("${alipay.sandbox.charset}")
    private String charset;

    @Value("${alipay.sandbox.sign-type}")
    private String signType;

    @Value("${alipay.sandbox.notify-url}")
    private String notifyUrl;

    @Value("${alipay.sandbox.return-url}")
    private String returnUrl;

    /**
     * 初始化AlipayClient
     * @return AlipayClient实例
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(gatewayUrl, appId, merchantPrivateKey, 
                "json", charset, alipayPublicKey, signType);
    }

    public String getAppId() {
        return appId;
    }

    public String getMerchantPrivateKey() {
        return merchantPrivateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public String getCharset() {
        return charset;
    }

    public String getSignType() {
        return signType;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }
} 