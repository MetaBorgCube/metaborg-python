# Milestone 1

In this first milestone we aimed to create an initial pipeline that could
convert a simple Python print statement to Web-Assembly.  This process is done
in several stages that are shortly described below.

## Parsing

We implemented parsing according to the official Python 3.6 syntax (as
described [here](https://docs.python.org/3/reference/grammar.html)).  This
official syntax uses a nested hierarchy to implement precedence rules. We
decided to follow this syntax, as it was the official way in which it is done,
despite that using the priorities in SDF3 could result in a more clean
solution. As suggested by Jasper, it might result in an interesting case study
to implement both ways and compare them. 

We focussed on getting a simple print statement to work but as the official
syntax is already fully described, we opted to write all the constructs
already. There are some problems with more complex programs, but for time
reasons we did not spend much time on improving the syntax (by for example
removing typos). This also meant that we did not fully investigate the layout
sensitive syntax. 

## Desugaring to AST

As the resulting syntax tree is deeply nested and overly complex, we have to
desugar the tree into a better AST. This AST is also the AST that Python uses
internally and is documented
[here](https://docs.python.org/3/library/ast.html).  For this desugaring step
we only worked towards supporting the aforementioned print statement, again for
time reasons.

The most complex program we can currently parse and desugar is of the form: 
```python
a.b.func(12, 13 & 14)
```

This converts the parsed syntax tree: 
```js
FileInput(
  [ Statement(
      SimpleStatement(
        [ Expression(
            Exp(
              [ XorExp(
                  [ AndExp(
                      [ ShiftExp(
                          ArithExp(
                            Term(
                              Power(
                                Power(
                                  AtomExp(
                                    None()
                                  , ID("a")
                                  , [ DotName("b")
                                    , DotName("func")
                                    , ArgList(
                                        Some(
                                          ArgList(
                                            [ CompForArgument(
                                                Test(
                                                  OrTest(
                                                    AndTest(
                                                      Comparison(
                                                        Exp(
                                                          [ XorExp(
                                                              [ AndExp(
                                                                  [ ShiftExp(
                                                                      ArithExp(
                                                                        Term(
                                                                          Power(Power(AtomExp(None(), Int("12"), []), None()))
                                                                        , []
                                                                        )
                                                                      , []
                                                                      )
                                                                    , []
                                                                    )
                                                                  ]
                                                                )
                                                              ]
                                                            )
                                                          ]
                                                        )
                                                      , []
                                                      )
                                                    , []
                                                    )
                                                  , None()
                                                  )
                                                , None()
                                                )
                                              , None()
                                              )
                                            , CompForArgument(
                                                Test(
                                                  OrTest(
                                                    AndTest(
                                                      Comparison(
                                                        Exp(
                                                          [ XorExp(
                                                              [ AndExp(
                                                                  [ ShiftExp(
                                                                      ArithExp(
                                                                        Term(
                                                                          Power(Power(AtomExp(None(), Int("13"), []), None()))
                                                                        , []
                                                                        )
                                                                      , []
                                                                      )
                                                                    , []
                                                                    )
                                                                  , ShiftExp(
                                                                      ArithExp(
                                                                        Term(
                                                                          Power(Power(AtomExp(None(), Int("14"), []), None()))
                                                                        , []
                                                                        )
                                                                      , []
                                                                      )
                                                                    , []
                                                                    )
                                                                  ]
                                                                )
                                                              ]
                                                            )
                                                          ]
                                                        )
                                                      , []
                                                      )
                                                    , []
                                                    )
                                                  , None()
                                                  )
                                                , None()
                                                )
                                              , None()
                                              )
                                            ]
                                          , None()
                                          )
                                        )
                                      )
                                    ]
                                  )
                                , None()
                                )
                              )
                            , []
                            )
                          , []
                          )
                        , []
                        )
                      ]
                    )
                  ]
                )
              ]
            )
          )
        ]
      , None()
      )
    , ""
    )
  ]
)
```

Into the desugared AST:

```js
Module(
  [ ExprStmt(
      Call(
        Attribute(
          Attribute(Name(ID("func"), Load()), ID("b"), Load())
        , ID("a")
        , Load()
        )
      , [Int("12"), BinOp(BitAnd(), Int("13"), Int("14"))]
      , []
      )
    )
  ]
)
```

## Name Binding Analysis

For the name binding analysis we started working on a scope graph. This scope
graph has the following structure for the simple print statement: ![Wcope
graph](img/M1-scope-graph.png)

At the moment the analysis is able to identify whether the called function is
defined (which holds in the case of `print`). When it is not defined, a warning
is shown. This is because in Python you can never be sure that items are
undefined.  This scope graph also allows to infer that certain objects are
callable (by the presence of a `__call__` function).

### Desugaring

The name binding analysis needed to be extended by a small desugaring step.
This is because a function call with positional arguments only stores the
argument values. We added a rule that makes it that those arguments are now
identifiable by an indexed number. 

### WebAssembly

WebAssembly, being at the frontier of software development, is going through
stages of development with multiple specifications bodies. It was developed
with values as efficiency, safety and platform-agnosticy in mind.

For this reasons, I/O is not an integrated part of the language, and the
language is mainly instanciated by the means of a JavaScript API. In order to
log output to the console, a helper function was written.

```js
const logStringFactory = memory => (position, length) => {
  const bytes = new Uint8Array(memory.buffer, position, length);
  const s = new TextDecoder('utf8').decode(bytes);
  console.log(s);
};

const memory = new WebAssembly.Memory({initial: 2});

WebAssembly.instantiateStreaming(fetch('hello.wasm'), {
  memory: {
    memory,
  },
  console,
  lib: {
    logString: logStringFactory(memory),
  },
}).then(obj => obj.instance.exports.main());
```

Build scripts etc. were developed to facilitate the deployment of WebAssembly
to the browser.

### WebAssembly Syntax Definition

Webassembly is organized into Modules, containing more of the following (in
that order)

- Types
- Imports
- Memory 
- Data
- Functions
- Start (calling functions)
- Exports

In order to fulfill the goal of printing strings, it was decided to implement
imports (to import the memory and the log function), data (to store the
string), functions (to be able to declare a main function) and exports (to be
able to export the said main function).

Each of these were declared in a consise way (there really wasn't that much
complexity, as the syntax of wast is quite straightforward), and only so much
as to be able to recognize and generate the program,

```wast
(module
  (func $_log (import "console" "log") )
  (func $_logString (import "lib" "logString") (param i32 i32))
  (memory $_memory (import "memory" "memory") 1)
  (data (i32.const 0) "Hello, Rasmus!\n") ;; str1
  (data (i32.const 15) "Hello, Chiel!\n") ;; str2

  (func $main 
    (i32.const 0) ;; str1*
    (i32.const 14) ;; len(str1)
    (call $_log)
    (i32.const 0) ;; str*
    (i32.const 14) ;; len(str)
    (call $_log)
  )
  (export "main" (func $main))
)
```

### Transformation to Wasm AST

Due to incredible difficulties importing the WebAssembly modules into the
Wython project, and lack of progress in that area, it was decided (for the sake
of demonstrability), to copy over the most important files to the Wython
project as an emergency solution. This was commenced quite late, because the
hope was still retained that the import problem (which can hardly be seen as an
interesting matter of language design) would be resolved before the deadline of
the first milestone.

Thus, the only action supported in the current AST transformation is that of
printing a string value. Once the import issue is resolved, the rest will be
implemented.

### Pretty-print to Wasm

As the copying of files described loses access to the generated pretty-printing
functions in the WebAssembly project, however. Thus, the compilation to
WebAssembly AST and the pretty-printing of such ASTs into actual codes are, at
the time of this writing, discrete, instead of a smooth pipeline. 

This will have to be improved in the upcoming milestone.


