package ru.windcorp.jputil.functions;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {

	void accept(T t) throws E;
	
	@SuppressWarnings("unchecked")
	default Consumer<T> withHandler(BiConsumer<? super T, ? super E> handler) {
		return t -> {
			try {
				accept(t);
			} catch (Exception e) {
				handler.accept(t, (E) e);
			}
		};
	}
	
	public static <T, E extends Exception> ThrowingConsumer<T, E> concat(ThrowingConsumer<? super T, ? extends E> first, ThrowingConsumer<? super T, ? extends E> second) {
		return t -> {
			first.accept(t);
			second.accept(t);
		};
	}
	
	public static <T, E extends Exception> ThrowingConsumer<T, E> concat(Consumer<? super T> first, ThrowingConsumer<? super T, ? extends E> second) {
		return t -> {
			first.accept(t);
			second.accept(t);
		};
	}
	
	public static <T, E extends Exception> ThrowingConsumer<T, E> concat(ThrowingConsumer<? super T, ? extends E> first, Consumer<? super T> second) {
		return t -> {
			first.accept(t);
			second.accept(t);
		};
	}
	
}
