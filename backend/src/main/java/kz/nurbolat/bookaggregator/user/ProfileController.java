package kz.nurbolat.bookaggregator.user;

import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.user.dto.UpdateProfileRequest;
import kz.nurbolat.bookaggregator.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    UserResponse get() {
        return profileService.get();
    }

    @PatchMapping
    UserResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(request);
    }
}
