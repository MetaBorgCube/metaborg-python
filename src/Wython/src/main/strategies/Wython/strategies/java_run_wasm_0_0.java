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

/**
 * External Stratego strategy to run .wasm files.
 */
public class java_run_wasm_0_0 extends Strategy {
  
  /**
   * Inner class to capture the console output.
   */
  public class Console {
    private StringBuilder out = new StringBuilder();
    
    /**
     * Callback for logging.
     * @param text: the logged text
     */
    public void log(String text){
            out.append(text);
            out.append('\n');
        }
  }
  
  private static final String BUILD_FOLDER = "/tmp/spoofax/wasm";
  public static java_run_wasm_0_0 instance = new java_run_wasm_0_0();
  
  /**
   * Main strategy that runs the given binary.
   */
  @Override
  public IStrategoTerm invoke(Context context, IStrategoTerm current) {
    try {
      context.getIOAgent().printError("Running wasm");
      String wasm_raw = current.toString();
      wasm_raw = wasm_raw.substring(1, wasm_raw.length() - 1).replace("\\n", "\n").replace("\\", ""); // Sanitize inputs
      
      // Setup the run environment
      setupBuildFolder();
      writeWasm(wasm_raw);
      
      // Actually run the code
      String out = run();
      
      ITermFactory factory = context.getFactory();
      return factory.makeString(out);
    } catch (Exception e) {
      ITermFactory factory = context.getFactory();
      return factory.makeString(e.getMessage());
    }
  }

  /**
   * Write the executable to a file.
   * 
   * @param wasm_raw: the executable
   * @throws IOException when writing the file fails
   */
  private void writeWasm(String wasm_raw) throws IOException {
    File wasm = new File(BUILD_FOLDER + "/out.wasm");
    writeFile(wasm, wasm_raw);
  }
  
  /**
   * Setup the build folder to contain all the needed files.
   * 
   * @throws IOException when creating the files fails
   */
  private void setupBuildFolder() throws IOException {
    File folder = new File(BUILD_FOLDER);
    if (!folder.exists()) folder.mkdirs();
    
    writeFile(new File(BUILD_FOLDER + "/script.js"), buildJS());
    writeFile(new File(BUILD_FOLDER + "/index.html"), buildHTML());
  }
  
  /**
   * Write (and overwrite) the given file.
   * 
   * @param file: the file to write
   * @param content: the contents of the file
   * @throws IOException when writing the file fails
   */
  private void writeFile(File file, String content) throws IOException {
    if (file.exists()) file.delete();
    try (BufferedWriter br = new BufferedWriter(new FileWriter(file))) {
      br.write(content);
    }
  }

  /**
   * Run the Webassembly code within a Javascript environment.
   * @return
   */
  private String run() {
     ScriptEngineManager factory = new ScriptEngineManager();
     //FIXME: Not sure how to get V8 to work
     String engineName = "jav8";
//     String engineName = "JavaScript";
     
     ScriptEngine engine = factory.getEngineByName(engineName);
     if (engine == null) {  // Engine not available
       return "ScriptEngine " + engineName + " not available. "
       + "Did you add the jav8-jsr223-xxx.jar to your classpath?\n"
       + "For now just start a http-server in the build folder (" + BUILD_FOLDER + ")";
     } else {
       try {
         // Capture console.log calls
         Console console = new Console();
         engine.put("console", console);
         
         //TODO: insert correct code here
         engine.eval("console.log(42)");
         return console.out.toString();
       } catch (ScriptException ex) {
         return ex.getMessage();
       }
     }
  }

  /**
   * @return the Javascript file that wraps the Webassembly code.
   */
  private String buildJS() {
    return  "const logStringFactory = memory => (position, length) => {\n" + 
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
           "debug: {\n" + 
           "  debugger: (n) => {\n" + 
           "    console.log(n);\n" + 
           "    debugger;\n" + 
           "  },\n" + 
           "},\n" + 
        "  lib: {\n" + 
        "    logString: logStringFactory(memory),\n" + 
        "  },\n" + 
        "});\n";
  }
  
  /**
   * @return the HTML file that is needed to show it in the browser
   */
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
