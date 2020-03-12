package ru.windcorp.jputil;

import java.util.HashMap;
import java.util.Map;

public class PrimitiveUtil {
	
	private PrimitiveUtil() {}

	private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_BOXED = new HashMap<>();
	private static final Map<Class<?>, Object> PRIMITIVE_TO_NULL = new HashMap<>();
	
	static {
		for (Class<?> boxed : new Class<?>[] {
			Boolean.class, Byte.class, Short.class, Character.class,
			Integer.class, Long.class, Float.class, Double.class
		}) {
			try {
				PRIMITIVE_TO_BOXED.put((Class<?>) boxed.getField("TYPE").get(null), boxed);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		PRIMITIVE_TO_NULL.put(Boolean.TYPE,		Boolean.FALSE);
		PRIMITIVE_TO_NULL.put(Byte.TYPE,		Byte.valueOf((byte) 0));
		PRIMITIVE_TO_NULL.put(Short.TYPE,		Short.valueOf((short) 0));
		PRIMITIVE_TO_NULL.put(Integer.TYPE,		Integer.valueOf(0));
		PRIMITIVE_TO_NULL.put(Long.TYPE,		Long.valueOf(0));
		PRIMITIVE_TO_NULL.put(Float.TYPE,		Float.valueOf(Float.NaN));
		PRIMITIVE_TO_NULL.put(Double.TYPE,		Double.valueOf(Double.NaN));
		PRIMITIVE_TO_NULL.put(Character.TYPE,	Character.valueOf('\u0000'));
	}
	
	public static Class<?> getBoxedClass(Class<?> primitiveClass) {
		return PRIMITIVE_TO_BOXED.getOrDefault(primitiveClass, primitiveClass);
	}
	
	public static Object getPrimitiveNull(Class<?> primitiveClass) {
		return PRIMITIVE_TO_NULL.get(primitiveClass);
	}
	
}
