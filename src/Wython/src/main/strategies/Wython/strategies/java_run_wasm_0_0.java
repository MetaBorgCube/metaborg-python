package Wython.strategies;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class java_run_wasm_0_0 extends Strategy {
	
	public class Console {
		private StringBuilder out = new StringBuilder();
		public void log(String text){
            out.append(text);
            out.append('\n');
        }
		public String getOut() {
			return out.toString();
		}
	}
	
	private static final String BUILD_FOLDER = "/tmp/spoofax/wasm";
	public static java_run_wasm_0_0 instance = new java_run_wasm_0_0();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		try {
			context.getIOAgent().printError("Running wasm");
			String wasm_raw = current.toString();
			wasm_raw = wasm_raw.substring(1, wasm_raw.length() - 1).replace("\\n", "\n").replace("\\", "");
			
			setupBuildFolder();
			writeWasm(wasm_raw);
			
			String out = run();
			
			ITermFactory factory = context.getFactory();
			return factory.makeString(out);
		} catch (Exception e) {
			ITermFactory factory = context.getFactory();
			return factory.makeString(e.getMessage());
		}
	}

	private void writeWasm(String wasm_raw) throws IOException {
		File wasm = new File(BUILD_FOLDER + "/out.wasm");
		writeFile(wasm, wasm_raw);
	}
	
	private void setupBuildFolder() throws IOException {
		File folder = new File(BUILD_FOLDER);
		if (!folder.exists()) folder.mkdirs();
		
		writeFile(new File(BUILD_FOLDER + "/script.js"), buildJS());
		writeFile(new File(BUILD_FOLDER + "/index.html"), buildHTML());
	}
	
	private void writeFile(File file, String content) throws IOException {
		if (file.exists()) file.delete();
		try (BufferedWriter br = new BufferedWriter(new FileWriter(file))) {
			br.write(content);
		}
	}

	private String run() {
		 ScriptEngineManager factory = new ScriptEngineManager();
		 String engineName = "jav8";
//		 String engineName = "JavaScript";
		 
		 ScriptEngine engine = factory.getEngineByName(engineName);
		 if (engine == null) {
			 String availableEngines = "";
			 for (ScriptEngineFactory ae: factory.getEngineFactories()) {
				 availableEngines += ae.getEngineName() + '\n';
			 }
			 return "ScriptEngine " + engineName + " not available. Available engines are: " + availableEngines
			 + "Did you add the jav8-jsr223-xxx.jar to your classpath?\n"
			 + "For now just start a server in the build folder (" + BUILD_FOLDER + ")";
		 } else {
			 try {
				 Console console = new Console();
				 engine.put("console", console);
				 engine.eval("console.log(42)");
				 return console.getOut();
			 } catch (ScriptException ex) {
				 return ex.getMessage();
			 }
		 }
	}

	private String buildJS() {
		return	"const logStringFactory = memory => (position, length) => {\n" + 
				"  const bytes = new Uint8Array(memory.buffer, position, length);\n" + 
				"  const s = new TextDecoder('utf8').decode(bytes);\n" + 
				"  console.log(s);\n" + 
				"};\n" + 
				"\n" + 
				"const memory = new WebAssembly.Memory({initial: 2});\n" + 
				"\n" + 
				"WebAssembly.instantiateStreaming(fetch('out.wasm'), {\n" + 
				"  memory: {\n" + 
				"    memory,\n" + 
				"  },\n" + 
				"  console,\n" + 
				"  lib: {\n" + 
				"    logString: logStringFactory(memory),\n" + 
				"  },\n" + 
				"}).then(obj => obj.instance.exports.main());\n";
	}
	
	private String buildHTML() {
		return  "<!DOCTYPE html>\n" + 
				"  <html lang=\"en\">\n" + 
				"  <head>\n" + 
				"    <meta charset=\"UTF-8\">\n" + 
				"    <title></title>\n" + 
				"  </head>\n" + 
				"  <body>\n" + 
				"    <script src=\"script.js\">\n" + 
				"    </script>\n" + 
				"  </body>\n" + 
				"</html>\n";
	}
}
