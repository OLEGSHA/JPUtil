package ru.windcorp.jputil.unixarg;

import java.lang.reflect.InvocationTargetException;

import ru.windcorp.jputil.unixarg.UnixArguments.*;

public class UnixArgumentBuilder<T> {
	
	private final Class<T> argumentType;
	private final Action<T> action;

	private String name = null;
	private Character letter = null;
	private String description = null;
	private boolean isRequired = false;
	private boolean isArgumentRequired = false;
	private boolean isSingleuse = false;
	
	
	
	public UnixArgumentBuilder(Class<T> argumentType, Action<T> action) {
		this.argumentType = argumentType;
		this.action = action;
	}
	
	public UnixArgumentBuilder<T> withDescription(String description) {
		this.description = description;
		return this;
	}
	
	public UnixArgumentBuilder<T> withName(String name) {
		this.name = name;
		return this;
	}
	
	public UnixArgumentBuilder<T> withLetter(char letter) {
		this.letter = letter;
		return this;
	}
	
	public UnixArgumentBuilder<T> required() {
		this.isRequired = true;
		return this;
	}
	
	public UnixArgumentBuilder<T> parameterRequired() {
		this.isArgumentRequired = true;
		return this;
	}
	
	public UnixArgumentBuilder<T> singleuse() {
		this.isSingleuse = true;
		return this;
	}
	
	
	
	public UnixArgument<T> build() {
		return new UnixArgument<T>(
				name,
				letter,
				description,
				argumentType,
				isRequired,
				isArgumentRequired,
				isSingleuse
		) {
			@Override
			protected boolean runImpl(T arg) throws UnixArgumentInvalidSyntaxException, InvocationTargetException {
				try {
					Handle<T> handle = new Handle<>(this);
					action.run(arg, handle);
					return handle.shouldTerminate();
				} catch (UnixArgumentInvalidSyntaxException | InvocationTargetException e) {
					throw e;
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				}
			}
		};
	}

}
