# Milestone 2

In the second milestone the focus was to focus was to get a core language working through all stages of the compiler (syntax/desugar, analysis, codegen). This core language should idealy include not only basic expressions, but also control-flow statements, functions and objects. The initial plan was that Chiel focussed on the first two stages, while Rasmus worked on code generation. The codegen was however a lot harder than expected, which limited the scope of our core language and made that we left out the analysis stage.

## Syntax/Desugaring
All of the syntax definitions were already implemented in milestone 1. They were however untested and, as an effect, had a number of problems. These were discovered when implementing the desugaring for the core language and fixed accordingly.
As a result, the following code snippet (which includes most constructs from our intended core language) will correctly desugar to the official python AST structure. 
```python
class A (object):
  def __init__(self):
    self.func2 = lambda x, y=3: x + y
    print('init')
  def func(n):
  	  a = 0
  	  while a < 0:
  	  	print('hello')
A().func(5)
```

This then desugars to the following AST:
```
Module(
  [ ClassDef(
      ID("A")
    , [ID("object")]
    , []
    , [ FunctionDef(
          "__init__"
        , Arguments(
            [Arg(ID("self"), None())]
          , []
          , []
          , []
          , []
          , []
          )
        , [ Assign(
              Name(
                Attribute(Name(ID("func2"), Store()), ID("self"), Load())
              , Store()
              )
            , Lambda(
                Arguments(
                  [Arg("x", None())]
                , None()
                , ["y"]
                , [Int("3")]
                , []
                , []
                )
              , BinOp(Add(), ID("x"), ID("y"))
              )
            )
          , ExprStmt(
              Call(
                Name(ID("print"), Load())
              , [String("'init'")]
              , []
              )
            )
          ]
        , []
        , None()
        )
      , FunctionDef(
          "func"
        , Arguments(
            [Arg(ID("n"), None())]
          , []
          , []
          , []
          , []
          , []
          )
        , [ Assign(Name(ID("a"), Store()), Int("0"))
          , While(
              Compare(ID("a"), [Lt()], [Int("0")])
            , [ ExprStmt(
                  Call(
                    Name(ID("print"), Load())
                  , [String("'hello'")]
                  , []
                  )
                )
              ]
            , []
            )
          ]
        , []
        , None()
        )
      ]
    , []
    )
  , ExprStmt(
      Call(
        Attribute(
          Name(ID("func"), Load())
        , Call(Name(ID("A"), Load()), [], [])
        , Load()
        )
      , [Int("5")]
      , []
      )
    )
  ]
)
```

### Layout sensitive syntax
The biggest problem we encountered while writing the sytax/desugaring, was the layout sensitive parsing. In the [documentation](http://www.metaborg.org/en/latest/source/langdev/meta/lang/sdf3/reference.html#layout-sensitive-parsing) the process is clearly explained with examples simmilar to our usecase. Despite this, we had a lot of trouble getting even the examples to work. After contacting Eduardo, two problems were found. The first being that using labels to referece trees was broken in the workbench. Eduardo was able to fix this within a day and with the new nightly build, we were able to compile the code again. The second problem was that, even with these fixes, we were not able to enforece the alignment of elements. After looking through a lot of documentation, an example project provided by Eduardo showed a mismatch in the configuration file. The problem was that the layout sensitive syntax is only supported by the `java` backend, where we were still using the `c` backend.

Now we were able to implement the layout sensitive syntax. Implementing this identified a lst problem. In the case of an if, there is an optional else/elif that should align. In the current implementation of the layout-sensitive parser, enforcing this alignment will also enforce that the else/elif exists, making it required instead of optional. According to Eduardo, this is already on the roadmap, but currently not implemented. As a solution, we now have two rules, one with and one without an else/elif.

## Analysis
Work on the analysis was planned for the last week before the deadline, as it would be beneficial to have the full desugared syntax available. Due to the problems encountered in the codegen, it was decided that the analysis would be postponed in order to focuss on the codegen.

## Codegen
In the first milestone the focus was on facilitating compilation of a simple print statement. In the second assignment the scope was widened to include while loops and more complex expressions.
The main difficulty in writing the assembly code is that there is no direct output. Therefore a lot of time was spent deciding on how to debug whenever a loop got stuck. First we included the writing of statements that printed numbers and strings.
This worked but each solution ended up quite limited to its own domain, and a more general approach was sought.
Finally it was decided to delegate such debugging to a helper function in Javascript, and functions therefore were added to the build script.
Then it was the issue of the syntax itself, which was implemented in SDF3. Because this syntax was developed from the Ground Up by observation instead of by appealing to a standard, a test driven development cycle was followed.
The Spoofax Language Workbench up being very helpful for this, and it was able to be implemented without much difficulty.
The rest of the time was spent thinking about different memory and object models. 
With regards to code generation it was built on top of what had been done before. The functions for offset calculation and other calculations needed in the pipeline were already done, so we thought that the implementation of the new keywords and some additional levels would only require a few recursive strategies.

It turned out that there were a number of problems with thos pre-existing functions. Therefore it was decided to convert the code generation from a multi-pass transformation to a single-pass one. This made all the calculations way simpler, speeding up the development. 

The code generation is now somewhat working for more examples. This means that we can almost run the following program through our entire compiler:
```
def func(n):
  a = 0
  while a < n:
  	print("Hello!")
  	a = a + 1
  print("World!")
func(42)
```
This results in the following `.wast` code:
```
(module
  (func $print (import "lib" "logString") (param i32 i32))
  (func $debug (import "debug" "debugger") (param i32 i32))
  (memory $d (import "memory" "memory") 1)
  (data (i32.const 0) "Hello!")
  (data (i32.const 6) "World!")
  (func $func (param $n i32)
    (local $a i32)
    (set_local $a
      (i32.const 0)
    )
    (block
      (loop
        (call $print
          (i32.const 0)
          (i32.const 6)
        )
        (local $a i32)  // Extra varDef
        (set_local $a
          (i32.add
            (get_local $a)
            (i32.const 1)
          )
        )
        (br_if 0
          (i32.lt_s
            (get_local $a)
            (get_local $n)
          )
        )
        (br 1)
      )
    )
    (call $print
      (i32.const 6)
      (i32.const 6)
    )
  )
  (func $main 
    (call $func
      (i32.const 42)
    )
```

This code does have a small problem when assigning to a variable more than once. This is because there is currently no way to distinguish whether a variable is already declared. Our current solution is to handle each assignment as an declaration with a default value, which results in extra declarations. These break at compile-time, but the code runs smoothly when they are removed. Adding a proper fix for this problem should not be that hard, but we had no time to implement this.
A second problem is that we can only print strings, as a proper data model is not yet implemented.

### Spoofax integration
When we started working on Webassembly, Rasmus created some build scripts to enable us to compile `.wast` files to `.wasm` and insert some HTLP/JavaScript wrapper code such that we can look at the results in a browser using the developer view.  
Using these scripts to run our code worked fine, but was cumbersome. We therefore wanted to integrate these scripts into the Spoofax IDE. In order to implement this, we created external Stratego strategies in Java that act in a similar way as the scripts. At the moment, running the `run` commando in the IDE will result in the following error:
```
ScriptEngine jav8 not available.
Did you add the jav8-jsr223-xxx.jar to your classpath?
For now just start a server in the build folder (/tmp/spoofax/wasm)
```
This is because the strategy does two things, where the first results in an error and the second is a temporary measure to get it working. Idealy you want to be able to run the Webassembly code directly from within Java and get the results back for full integration with Spoofax. For this we need to run a V8 wrapper and get the console output back. The code to do this is already present, but as including the wrapper has some problems it now just throws the error shown above. The second thing it does is just writing all the files in the same way the old script did. This makes that you still have to open a http-server and view the results in the browser. Despite this, it is already a welcome improvement to the workflow.

### Data model
The internal workings of Python rely on objects that are basically hashmaps. When emulating this behaviour in Webassembly we therefore need a good object representation to work with. For now this is not implemented, as it was too much work. The signature of those objects should be of the form `[size, type, (size, nameSize, name, value)+]`. This structure allows to emulate the hashmap functionality (albeit slower) without the complexity of actually implementing a hashmap. We will probably start using this memory layout via some Javascript helper functions and later switch to a pure Webasembly solution.
