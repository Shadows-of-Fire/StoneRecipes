package shadows.stonerecipes.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Some reflection helper code.
 *
 * @author cpw
 *
 */
public class ReflectionHelper {
	public static class UnableToFindMethodException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		//private String[] methodNames;

		public UnableToFindMethodException(String[] methodNames, Exception failed) {
			super(failed);
			//this.methodNames = methodNames;
		}

		public UnableToFindMethodException(Throwable failed) {
			super(failed);
		}

	}

	public static class UnableToFindClassException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		//private String[] classNames;

		public UnableToFindClassException(String[] classNames, @Nullable Exception err) {
			super(err);
			//this.classNames = classNames;
		}

	}

	public static class UnableToAccessFieldException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		//private String[] fieldNameList;

		public UnableToAccessFieldException(String[] fieldNames, Exception e) {
			super(e);
			//this.fieldNameList = fieldNames;
		}
	}

	public static class UnableToFindFieldException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		//private String[] fieldNameList;
		public UnableToFindFieldException(String[] fieldNameList, Exception e) {
			super(e);
			//this.fieldNameList = fieldNameList;
		}
	}

	public static class UnknownConstructorException extends RuntimeException {
		public UnknownConstructorException(final String message) {
			super(message);
		}
	}

	public static Field findField(Class<?> clazz, String... fieldNames) {
		Exception failed = null;
		for (String fieldName : fieldNames) {
			try {
				Field f = clazz.getDeclaredField(fieldName);
				f.setAccessible(true);
				return f;
			} catch (Exception e) {
				failed = e;
			}
		}
		throw new UnableToFindFieldException(fieldNames, failed);
	}

	@SuppressWarnings("unchecked")
	public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, String... fieldNames) {
		try {
			return (T) findField(classToAccess, fieldNames).get(instance);
		} catch (Exception e) {
			throw new UnableToAccessFieldException(fieldNames, e);
		}
	}

	public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String... fieldNames) {
		try {
			Field f = findField(classToAccess, fieldNames);
			setFailsafeFieldValue(f, instance, value);
		} catch (Exception e) {
			throw new UnableToAccessFieldException(fieldNames, e);
		}
	}

	private static Object reflectionFactory = null;
	private static Method newFieldAccessor = null;
	private static Method fieldAccessorSet = null;
	private static boolean isSetup = false;

	private static void setup() {
		if (isSetup) { return; }

		try {
			Method getReflectionFactory = Class.forName("sun.reflect.ReflectionFactory").getDeclaredMethod("getReflectionFactory");
			reflectionFactory = getReflectionFactory.invoke(null);
			newFieldAccessor = Class.forName("sun.reflect.ReflectionFactory").getDeclaredMethod("newFieldAccessor", Field.class, boolean.class);
			fieldAccessorSet = Class.forName("sun.reflect.FieldAccessor").getDeclaredMethod("set", Object.class, Object.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		isSetup = true;
	}

	static {
		setup();
	}

	public static void setFailsafeFieldValue(Field field, @Nullable Object target, @Nullable Object value) throws Exception {
		field.setAccessible(true);
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		Object fieldAccessor = newFieldAccessor.invoke(reflectionFactory, field, false);
		fieldAccessorSet.invoke(fieldAccessor, target, value);
	}

	/**
	 * Finds a constructor in the specified class that has matching parameter types.
	 *
	 * @param klass The class to find the constructor in
	 * @param parameterTypes The parameter types of the constructor.
	 * @param <T> The type
	 * @return The constructor
	 * @throws NullPointerException if {@code klass} is null
	 * @throws NullPointerException if {@code parameterTypes} is null
	 * @throws UnknownConstructorException if the constructor could not be found
	 */
	@Nonnull
	public static <T> Constructor<T> findConstructor(@Nonnull final Class<T> klass, @Nonnull final Class<?>... parameterTypes) {
		Preconditions.checkNotNull(klass, "class");
		Preconditions.checkNotNull(parameterTypes, "parameter types");

		final Constructor<T> constructor;
		try {
			constructor = klass.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);
		} catch (final NoSuchMethodException e) {
			final StringBuilder desc = new StringBuilder();
			desc.append(klass.getSimpleName()).append('(');
			for (int i = 0, length = parameterTypes.length; i < length; i++) {
				desc.append(parameterTypes[i].getName());
				if (i > length) {
					desc.append(',').append(' ');
				}
			}
			desc.append(')');
			throw new UnknownConstructorException("Could not find constructor '" + desc.toString() + "' in " + klass);
		}
		return constructor;
	}
}