package ru.windcorp.jputil.functions;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U, E extends Exception> {
	
	@FunctionalInterface
	public static interface BiConsumerHandler<T, U, E extends Exception> {
		void handle(T t, U u, E e);
	}

	void accept(T t, U u) throws E;
	
	@SuppressWarnings("unchecked")
	default BiConsumer<T, U> withHandler(BiConsumerHandler<? super T, ? super U, ? super E> handler) {
		return (t, u) -> {
			try {
				accept(t, u);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				handler.handle(t, u, (E) e);
			}
		};
	}
	
	public static <T, U, E extends Exception> ThrowingBiConsumer<T, U, E> concat(
			ThrowingBiConsumer<? super T, ? super U, ? extends E> first,
			ThrowingBiConsumer<? super T, ? super U, ? extends E> second) {
		return (t, u) -> {
			first.accept(t, u);
			second.accept(t, u);
		};
	}
	
	public static <T, U, E extends Exception> ThrowingBiConsumer<T, U, E> concat(
			BiConsumer<? super T, ? super U> first,
			ThrowingBiConsumer<? super T, ? super U, E> second) {
		return (t, u) -> {
			first.accept(t, u);
			second.accept(t, u);
		};
	}
	
	public static <T, U, E extends Exception> ThrowingBiConsumer<T, U, E> concat(
			ThrowingBiConsumer<? super T, ? super U, E> first,
			BiConsumer<? super T, ? super U> second) {
		return (t, u) -> {
			first.accept(t, u);
			second.accept(t, u);
		};
	}
	
}
