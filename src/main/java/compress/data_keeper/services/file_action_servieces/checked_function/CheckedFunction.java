package compress.data_keeper.services.file_action_servieces.checked_function;

@FunctionalInterface
public interface CheckedFunction<T, R> {
    R apply(T t) throws Exception;
}
