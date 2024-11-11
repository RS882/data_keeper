package compress.data_keeper.services.file_action_servieces.checked_function;

import compress.data_keeper.exception_handler.server_exception.ServerIOException;

import java.util.function.Function;

public class WrapCheckedFunction {
    public static <T, R> Function<T, R> wrapCheckedFunction(CheckedFunction<T, R> checkedFunction) {
        return t -> {
            try {
                return checkedFunction.apply(t);
            } catch (Exception e) {
                throw new ServerIOException(e.getMessage());
            }
        };
    }
}
