package Wython.strategies;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * External Stratego strategy for compiling .wast to .wasm 
 */
public class java_compile_wast_0_0 extends Strategy {
	
	public static java_compile_wast_0_0 instance = new java_compile_wast_0_0();
	//TODO: Find a better way to do this
	private static final String WASM_COMPILER = new File("wabt/bin/wat2wasm").getAbsolutePath();

	/**
	 * Main strategy that converts the input, compiles the code and returns the result.
	 */
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		ITermFactory factory = context.getFactory();
		try {
			context.getIOAgent().printError("Compiling source file: " + current.getSubterm(0));
		
			String sourceFile = current.getSubterm(0).toString();
			File wastFile = new File(sourceFile.substring(1, sourceFile.length()-1));
			
			// Write wast file to compile (as you cannot pass stdin to the compiler)
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(wastFile))) {
				String content = current.getSubterm(1).toString();
				String out = content.substring(1,  content.length()-1).replace("\\n", "\n").replace("\\", "");	// Sanitize inputs
				bw.write(out);
			}
			
			String js = compileWasm(wastFile);
			return factory.makeString(js);
		} catch (Exception e) {
			context.getIOAgent().printError(e.getMessage());
			return factory.makeString("");
		}
	}

	/**
	 * Compile the given file to a .wasm-executable.
	 * 
	 * @param wastFile: This .wast-file to compile
	 * @return The compiled binary as a String
	 * @throws Exception when compilation fails
	 */
	private String compileWasm(File wastFile) throws Exception  {
		File outFile = new File(wastFile.getAbsolutePath().replace(".wast", ".wasm"));
		
		Runtime rt = Runtime.getRuntime();
		String[] commands = {WASM_COMPILER, wastFile.getAbsolutePath(), "-o", outFile.getAbsolutePath()};

		try {
			Process proc = rt.exec(commands);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			
			String s = null;
			StringBuilder out = new StringBuilder();
		
			while ((s = stdInput.readLine()) != null) {
			    out.append(s); out.append('\n');
			}
			while ((s = stdError.readLine()) != null) {
				out.append(s); out.append('\n');
			}
			
			if (out.length() > 0) {
				throw new Exception(out.toString());
			} else {
				StringBuilder wasm = new StringBuilder();
				try (FileReader fr = new FileReader(outFile)) {
					while (fr.ready()) {
						wasm.append((char) fr.read());
					}
				}
				return wasm.toString();
			}
		} catch (IOException e) {
			throw new Exception("Problem compiling wasm: " + e.getMessage());
		}
	}
}
