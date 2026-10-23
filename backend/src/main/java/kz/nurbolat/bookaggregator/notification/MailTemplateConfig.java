package kz.nurbolat.bookaggregator.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Plain-text versions of emails ({@code templates/email/*.txt}). Spring Boot adds this resolver to its template
 * engine next to the HTML one, which keeps resolving every name that does not end in {@code .txt}.
 */
@Configuration
class MailTemplateConfig {

    @Bean
    ITemplateResolver textEmailTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setResolvablePatterns(Set.of("email/*.txt"));
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setOrder(0);
        resolver.setCheckExistence(true);
        return resolver;
    }
}
