package kz.nurbolat.bookaggregator.common.error;

public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
