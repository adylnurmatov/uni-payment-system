package kg.alatoo.payment.auth;

import kg.alatoo.payment.AuthenticateMerchantPort;
import kg.alatoo.payment.models.Merchant;
import kg.alatoo.payment.rest.Headers;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentMerchantArgumentResolver implements HandlerMethodArgumentResolver {
    private final AuthenticateMerchantPort authenticateMerchant;

    public CurrentMerchantArgumentResolver(AuthenticateMerchantPort authenticateMerchant) {
        this.authenticateMerchant = authenticateMerchant;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMerchant.class)
                && Merchant.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Merchant resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return authenticateMerchant.execute(webRequest.getHeader(Headers.API_KEY));
    }
}
