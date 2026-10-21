package kz.nurbolat.bookaggregator.support;

import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Wraps the application DataSource so tests can count SQL statements per thread
 * ({@code QueryCountHolder}).
 */
@TestConfiguration(proxyBeanMethods = false)
public class QueryCountingConfig {

    @Bean
    static BeanPostProcessor queryCountingDataSource() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof ProxyDataSource)) {
                    return ProxyDataSourceBuilder.create(dataSource).name("app").countQuery().build();
                }
                return bean;
            }
        };
    }
}
