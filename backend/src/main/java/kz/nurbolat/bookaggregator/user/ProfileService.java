package kz.nurbolat.bookaggregator.user;

import kz.nurbolat.bookaggregator.security.SecurityUtils;
import kz.nurbolat.bookaggregator.user.dto.UpdateProfileRequest;
import kz.nurbolat.bookaggregator.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public UserResponse get() {
        return UserResponse.from(securityUtils.getCurrentUser());
    }

    @Transactional
    public UserResponse update(UpdateProfileRequest request) {
        User user = securityUtils.getCurrentUser();
        if (request.name() != null) {
            user.setName(request.name().strip());
        }
        if (request.notifyFrequency() != null) {
            user.setNotifyFrequency(request.notifyFrequency());
        }
        if (request.weeklyDigest() != null) {
            user.setWeeklyDigest(request.weeklyDigest());
        }
        return UserResponse.from(user);
    }
}
