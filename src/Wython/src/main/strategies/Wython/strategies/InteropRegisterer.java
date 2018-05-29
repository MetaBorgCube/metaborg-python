package Wython.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

public class InteropRegisterer extends JavaInteropRegisterer {
    public InteropRegisterer() {
        super(new Strategy[] { java_run_wasm_0_0.instance, java_compile_wast_0_0.instance });
    }
}
