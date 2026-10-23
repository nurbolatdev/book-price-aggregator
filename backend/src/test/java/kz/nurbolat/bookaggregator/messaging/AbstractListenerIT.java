package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Listener tests: queue consumers are started (they are off in other tests) and the crawler is a mock,
 * so no store is ever requested. The context is closed after each class: its consumers would otherwise keep
 * taking messages from queues that other tests read.
 */
@TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractListenerIT extends AbstractIntegrationTest {

    @MockBean
    protected StoreCrawlService crawlService;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected RabbitAdmin rabbitAdmin;

    @Autowired
    protected MessagingProperties messagingProperties;

    @BeforeEach
    void emptyQueues() {
        rabbitAdmin.purgeQueue(RabbitTopology.PARSE_FLIP_QUEUE);
        rabbitAdmin.purgeQueue(messagingProperties.deadLetterQueue());
    }
}
