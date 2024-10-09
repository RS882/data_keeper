package compress.data_keeper.exception_handler.unauthorized;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("User email or password is wrong");
    }
}
