package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.admin.SystemEvent.Level;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Writes events for the admin journal. Each event is committed on its own, so it survives
 * a rollback of the operation that reported it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemEventService {

    private final SystemEventRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void info(String source, String message, Map<String, Object> details) {
        save(Level.INFO, source, message, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void warn(String source, String message, Map<String, Object> details) {
        save(Level.WARN, source, message, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void error(String source, String message, Map<String, Object> details) {
        save(Level.ERROR, source, message, details);
    }

    private void save(Level level, String source, String message, Map<String, Object> details) {
        SystemEvent event = new SystemEvent();
        event.setLevel(level);
        event.setSource(source);
        event.setMessage(message);
        event.setDetails(details == null || details.isEmpty() ? null : details);
        repository.save(event);
    }
}
