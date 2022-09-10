package com.github.cm360.cwplugin.integrations.playtime.awards;

import java.util.stream.Stream;

public class AwardFactory {

	private Class<? extends PlayTimeAward> awardClass;
	private Object[] arguments;
	
	public AwardFactory(Class<? extends PlayTimeAward> awardClass, Object... arguments) {
		this.awardClass = awardClass;
		this.arguments = arguments;
	}
	
	public PlayTimeAward create(long timeNeeded) {
		try {
			// Add Long.class to the list of argument classes
			Class<?>[] argumentsClasses = new Class<?>[arguments.length + 1];
			argumentsClasses[0] = Long.class;
			System.arraycopy(Stream.of(arguments).map(arg -> arg.getClass()).toArray(Class<?>[]::new), 0, argumentsClasses, 1, arguments.length);
			// Add all arguments to an array
			Object[] argumentsAll = new Object[arguments.length + 1];
			argumentsAll[0] = timeNeeded;
			System.arraycopy(arguments, 0, argumentsAll, 1, arguments.length);
			// Create the PlayTimeAward object
			return awardClass.getConstructor(argumentsClasses).newInstance(argumentsAll);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
