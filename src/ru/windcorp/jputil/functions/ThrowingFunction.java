package ru.windcorp.jputil.functions;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {

	R apply(T t) throws E;
	
	@SuppressWarnings("unchecked")
	default Function<T, R> withHandler(BiConsumer<? super T, ? super E> handler, Function<? super T, ? extends R> value) {
		return t -> {
			try {
				return apply(t);
			} catch (Exception e) {
				if (handler != null) handler.accept(t, (E) e);
				return value == null ? null : value.apply(t);
			}
		};
	}
	
	default Function<T, R> withHandler(BiConsumer<? super T, ? super E> handler, Supplier<? extends R> value) {
		return withHandler(handler, t -> value.get());
	}
	
	default Function<T, R> withHandler(BiConsumer<? super T, ? super E> handler, R value) {
		return withHandler(handler, t -> value);
	}
	
	default Function<T, R> withHandler(BiConsumer<? super T, ? super E> handler) {
		return withHandler(handler, (Function<T, R>) null);
	}
	
	public static <T, R, I, E extends Exception> ThrowingFunction<T, R, E> compose(
			ThrowingFunction<? super T, I, ? extends E> first,
			ThrowingFunction<? super I, ? extends R, ? extends E> second) {
		return t -> second.apply(first.apply(t));
	}
	
	public static <T, R, I, E extends Exception> ThrowingFunction<T, R, E> compose(
			Function<? super T, I> first,
			ThrowingFunction<? super I, ? extends R, E> second) {
		return t -> second.apply(first.apply(t));
	}
	
	public static <T, R, I, E extends Exception> ThrowingFunction<T, R, E> compose(
			ThrowingFunction<? super T, I, E> first,
			Function<? super I, ? extends R> second) {
		return t -> second.apply(first.apply(t));
	}
	
}
