package ru.windcorp.jputil.unixarg;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

public class UnixArguments {
	
	private UnixArguments() {}
	
	/*
	 *  Action, SimpleAction, VoidAction, VoidSimpleAction:
	 *  
	 *  SonarLint: Generic exceptions should never be thrown (java:S112)
	 *    The argument system is designed to handle arbitrary exceptions so as not to bloat client code with
	 *    exception handling
	 *  SonarLint: "throws" declarations should not be superfluous (java:S1130)
	 *    The declarations a reminder that the superfluous exceptions must be handled differently
	 */ 
	
	@FunctionalInterface
	public static interface Action<T> {
		
		/**
		 * Performs the action.
		 * @param argument the argument received or {@code null} if none
		 * @param handle context of the invocation
		 * @throws UnixArgumentInvalidSyntaxException if the argument has malformed syntax.
		 * @throws InvocationTargetException if an exception has occurred and has been handled appropriately already.
		 * @throws Exception if an unexpected exception has occurred. This will be handled by the argument system.
		 */
		@SuppressWarnings({"squid:S112", "squid:S1130"})
		void run(T argument, Handle<T> handle)
				throws UnixArgumentInvalidSyntaxException,
				       InvocationTargetException,
				       Exception;
	}
	
	@FunctionalInterface
	public static interface SimpleAction<T> {
		
		/**
		 * Performs the action.
		 * @param argument the argument received or {@code null} if none
		 * @throws UnixArgumentInvalidSyntaxException if the argument has malformed syntax.
		 * @throws InvocationTargetException if an exception has occurred and has been handled appropriately already.
		 * @throws Exception if an unexpected exception has occurred. This will be handled by the argument system.
		 */
		@SuppressWarnings({"squid:S112", "squid:S1130"})
		void run(T argument)
				throws UnixArgumentInvalidSyntaxException,
				       InvocationTargetException,
				       Exception;
	}
	
	@FunctionalInterface
	public static interface VoidAction {
		
		/**
		 * Performs the action.
		 * @param handle context of the invocation
		 * @throws UnixArgumentInvalidSyntaxException if the argument has malformed syntax.
		 * @throws InvocationTargetException if an exception has occurred that has been handled appropriately already.
		 * @throws Exception if an unexpected exception has occurred. This will be handled by the argument system.
		 */
		@SuppressWarnings({"squid:S112", "squid:S1130"})
		void run(Handle<Void> handle)
				throws UnixArgumentInvalidSyntaxException,
				       InvocationTargetException,
				       Exception;
	}
	
	@FunctionalInterface
	public static interface VoidSimpleAction {
		
		/**
		 * Performs the action.
		 * @throws UnixArgumentInvalidSyntaxException if the argument has malformed syntax.
		 * @throws InvocationTargetException if an exception has occurred and has been handled appropriately already.
		 * @throws Exception if an unexpected exception has occurred. This will be handled by the argument system.
		 */
		@SuppressWarnings({"squid:S112", "squid:S1130"})
		void run()
				throws UnixArgumentInvalidSyntaxException,
				       InvocationTargetException,
				       Exception;
	}
	
	public static class Handle<T> {
		private boolean shouldTerminate = false;
		private final UnixArgument<T> argument;
		
		public Handle(UnixArgument<T> argument) {
			this.argument = argument;
		}
		
		public UnixArgument<T> getArgument() {
			return argument;
		}
		
		public void terminate() {
			this.shouldTerminate = true;
		}
		
		public boolean shouldTerminate() {
			return shouldTerminate;
		}
	}
	
	public static <T> UnixArgumentBuilder<T> forAction(
			Class<T> argumentType,
			Action<T> action
	) {
		return new UnixArgumentBuilder<>(argumentType, action);
	}
	
	public static UnixArgumentBuilder<Void> forAction(
			VoidAction action
	) {
		return new UnixArgumentBuilder<>(Void.TYPE, (argument, handle) -> action.run(handle));
	}
	
	public static <T> UnixArgumentBuilder<T> forAction(
			Class<T> argumentType,
			SimpleAction<T> action
	) {
		return forAction(argumentType, (argument, handle) -> action.run(argument));
	}
	
	public static UnixArgumentBuilder<Void> forAction(
			VoidSimpleAction action
	) {
		return forAction(argument -> action.run());
	}
	
	public static <T> UnixArgumentBuilder<String> forCollection(
			Collection<T> options,
			Consumer<T> action,
			BiPredicate<T, String> comparator,
			String errorMessageFormat
	) {
		return forAction(String.class, (argument, handle) ->
			action.accept(options.stream()
				.filter(option -> comparator.test(option, argument))
				.findAny()
				.orElseThrow(() -> new UnixArgumentInvalidSyntaxException(
						String.format(errorMessageFormat, argument),
						handle
				)
			))
		);
	}
	
	public static <T> UnixArgumentBuilder<String> forCollection(
			Collection<T> options,
			Consumer<T> action,
			Function<T, String> keyer,
			String errorMessageFormat
	) {
		return forCollection(
				options,
				action,
				(object, input) -> input.equalsIgnoreCase(keyer.apply(object)),
				errorMessageFormat
		);
	}
	
	public static <T extends Enum<T>> UnixArgumentBuilder<String> forEnum(
			Class<T> enumType,
			Consumer<T> action,
			BiPredicate<T, String> comparator,
			String errorMessage
	) {
		return forCollection(
				Arrays.asList(enumType.getEnumConstants()),
				action,
				comparator,
				errorMessage
		);
	}
	
	public static <T extends Enum<T>> UnixArgumentBuilder<String> forEnum(
			Class<T> enumType,
			Consumer<T> action,
			Function<T, String> keyer,
			String errorMessage
	) {
		return forCollection(
				Arrays.asList(enumType.getEnumConstants()),
				action,
				keyer,
				errorMessage
		);
	}
	
	public static <T extends Enum<T>> UnixArgumentBuilder<String> forEnum(
			Class<T> enumType,
			Consumer<T> action,
			String errorMessage
	) {
		return forEnum(
				enumType,
				action,
				constant -> Enum.class.cast(constant).name(),
				errorMessage
		);
	}

}
