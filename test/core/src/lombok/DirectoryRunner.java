/*
 * Copyright © 2009-2010 Reinier Zwitserloot and Roel Spilker.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class DirectoryRunner extends Runner {
	public enum Compiler {
		DELOMBOK, JAVAC, ECJ;
	}
	
	public interface TestParams {
		Compiler getCompiler();
		boolean printErrors();
		File getBeforeDirectory();
		File getAfterDirectory();
		File getMessagesDirectory();
	}
	
	private static final FileFilter JAVA_FILE_FILTER = new FileFilter() {
		@Override public boolean accept(File file) {
			return file.isFile() && file.getName().endsWith(".java");
		}
	};
	
	private final Description description;
	private final Map<String, Description> tests = new TreeMap<String, Description>();
	private final Throwable failure;
	private final TestParams params;
	
	public DirectoryRunner(Class<?> testClass) throws Exception {
		description = Description.createSuiteDescription(testClass);
		
		this.params = (TestParams) testClass.newInstance();
		
		Throwable error = null;
		try {
			addTests(testClass);
		}
		catch (Throwable t) {
			error = t;
		}
		this.failure = error;
	}
	
	private void addTests(Class<?> testClass) throws Exception {
		for (File file : params.getBeforeDirectory().listFiles(JAVA_FILE_FILTER)) {
			Description testDescription = Description.createTestDescription(testClass, file.getName());
			description.addChild(testDescription);
			tests.put(file.getName(), testDescription);
		}
	}
	
	@Override
	public Description getDescription() {
		return description;
	}
	
	@Override
	public void run(RunNotifier notifier) {
		if (failure != null) {
			notifier.fireTestStarted(description);
			notifier.fireTestFailure(new Failure(description, failure));
			notifier.fireTestFinished(description);
			return;
		}
		
		for (Map.Entry<String, Description> entry : tests.entrySet()) {
			Description testDescription = entry.getValue();
			notifier.fireTestStarted(testDescription);
			try {
				if (!runTest(entry.getKey())) {
					notifier.fireTestIgnored(testDescription);
				}
			}
			catch (Throwable t) {
				notifier.fireTestFailure(new Failure(testDescription, t));
			}
			notifier.fireTestFinished(testDescription);
		}
	}
	
	private boolean runTest(String fileName) throws Throwable {
		File file = new File(params.getBeforeDirectory(), fileName);
		if (mustIgnore(file)) {
			return false;
		}
		switch (params.getCompiler()) {
		case DELOMBOK:
			return new RunTestsViaDelombok().compareFile(params, file);
		case ECJ:
			return new RunTestsViaEcj().compareFile(params, file);
		default:
		case JAVAC:
			throw new UnsupportedOperationException();
		}
	}
	
	private boolean mustIgnore(File file) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		reader.close();
		return "//ignore".equals(line);
	}
}
