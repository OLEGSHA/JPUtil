package ru.windcorp.jputil.functions;

import java.util.function.Consumer;
import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {

	T get() throws E;
	
	@SuppressWarnings("unchecked")
	default Supplier<T> withHandler(Consumer<? super E> handler, Supplier<? extends T> value) {
		return () -> {
			try {
				return get();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				if (handler != null) handler.accept((E) e);
				return value == null ? null : value.get();
			}
		};
	}
	
	default Supplier<T> withHandler(Consumer<? super E> handler, T value) {
		return withHandler(handler, () -> value);
	}
	
	default Supplier<T> withHandler(Consumer<? super E> handler) {
		return withHandler(handler, (Supplier<T>) null);
	}
	
}
