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
The biggest problem we encountered while writing the sytax/desugaring, was the layout sensitive parsing.

## Analysis

## Codegen

### Data model
