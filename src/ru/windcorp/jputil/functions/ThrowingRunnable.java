package ru.windcorp.jputil.functions;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {

	void run() throws E;
	
	@SuppressWarnings("unchecked")
	default Runnable withHandler(Consumer<? super E> handler) {
		return () -> {
			try {
				run();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				handler.accept((E) e);
			}
		};
	}
	
	public static <E extends Exception> ThrowingRunnable<E> concat(ThrowingRunnable<? extends E> first, ThrowingRunnable<? extends E> second) {
		return () -> {
			first.run();
			second.run();
		};
	}
	
	public static <E extends Exception> ThrowingRunnable<E> concat(Runnable first, ThrowingRunnable<E> second) {
		return () -> {
			first.run();
			second.run();
		};
	}
	
	public static <E extends Exception> ThrowingRunnable<E> concat(ThrowingRunnable<E> first, Runnable second) {
		return () -> {
			first.run();
			second.run();
		};
	}
	
}
