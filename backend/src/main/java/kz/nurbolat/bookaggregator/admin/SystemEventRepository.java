package kz.nurbolat.bookaggregator.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemEventRepository extends JpaRepository<SystemEvent, Long> {

    Page<SystemEvent> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<SystemEvent> findByLevelOrderByCreatedAtDescIdDesc(SystemEvent.Level level, Pageable pageable);
}
