package Wython.strategies;

import java.io.ByteArrayOutputStream;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class eval_javascript_0_0 extends Strategy {

	private static final ILogger logger = LoggerUtils.logger("Javascript interpreter");

	public static final eval_javascript_0_0 instance = new eval_javascript_0_0();
	
	
	private String evalAndCaptureOutput(ScriptEngine engine, String program) throws ScriptException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		//MergedWriter merged = new MergedWriter(engine.getContext().getWriter(), new OutputStreamWriter(bos));
		//engine.getContext().setWriter(merged);
		//engine.eval(program);
		//engine.getContext().setWriter(merged.lhs);
		return new String(bos.toByteArray());
	} 
	
	//@Override
	//public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		//String program = null;
		//if(current.getTermType() == IStrategoTerm.STRING){
			//program = ((IStrategoString) current).stringValue();
		//} else {
			//IStrategoTerm ppJsTerm = context.invokeStrategy("pp-js-program", current);
			//program = ((IStrategoString) ppJsTerm).stringValue();
		//}
		//ScriptEngine engine = NashornInitializer.getInstance(context);
		//try {
			//String result = evalAndCaptureOutput(engine, program);
			//return context.getFactory().makeString(result);
		//} catch (ScriptException e) {
			//logger.error("", e);
			//context.popOnFailure();
			//return current;
		//}
	//}
}
