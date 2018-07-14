# Milestone 3

In the third milestone the focus was mainly on increasing the coverage of the
language. It was expected that development on the Webassembly backend would be
slow, therefore it was deemed very likely that the code analysis would achieve
a higher coverage. In the following sections our progress is explained for the
three main areas of the code.

## Syntax/Desugaring

It was not planned to spend a lot of time on the syntax and its desugaring. We
would make some small fixes for known issues, but no addition of language
constructs. It turned out that this was a false assumption and that we had to
spend a lot of time on this again. The first thing that we needed to do was to
add support for augmented assigns (+= -=), as this construct was simply
forgotten. Luckily this was not much work to integrate.

A second thing that needed fixing were the else branches of the `if` and
`while` statements. Making this fix was however not trivial as the root of the
problem was not clear, all rules to parse them were present. Around that same
time Eduardo messaged Chiel to ask how far our implementation of the syntax
was, as he might want to use it as part of a benchmark for a paper he is
currently writing. He identified a number of issues that could cause our
problems. The main cause was that we excluded newlines from our layout. This
was still an artifact of milestone 1 and 2 where we tried to get the
layout-sensitive syntax to work. With this change made, we fixed the problems
we had. There was one downside however, as we introduced a lot of ambiguities
with this new version. They were however hard to fix as it was not clear how
they were ambiguous. An example of this can be seen in the image below.

![Abiguous syntax](img/M3-old-syntax.png)

We were not able to fix the issues quickly, therefore we decided for a dirty
solution that would work fine. We created an extra desugaring step to simply
filter all the `amb()` constructs and replace them by the first element in
their list of possibilities. This worked fine, but quickly became annoying
when writing the NaBL2 analysis. Due to the ambiguities the analysis result
would still contain a giant warning showing the ambiguous AST, which cluttered
the output. To fix this it was decided to rewrite the entire syntax. It was
already planned that we wanted to create a second syntax for Python, one the
offical way and one using the SDF3 constructs. This would result in an
interesting case study to see the difference between both implementations. Now
we just had to to it earlier in the project.

The new syntax was relatively easy to create, as we had a reference
implementation in our old syntax definition. With the use of SDF3 constructs
for priorities and associativity, the new syntax is way more readable and
clean. This is the clearest in the case of the syntax for expressions, for
which the before and after can be seen in the images below.

![Old syntax](img/M3-old-sdf.png)

![New syntax](img/M3-new-sdf.png)

An effect of the new syntax was that we needed to update the desugaring in
order to transform the new parse-AST to the Python AST. This was not much work
as the new syntax results in a cleaner AST that is more easily desugared. We
also made small changes to the official AST format in the case of binary
expressions. They are now of the form `BinOp(Op, LeftExp, RightExp)`. This not
only makes desugaring more straightforward, but also simplifies the analysis
and code generation.

With this syntax we already passed more tests than with the old syntax, but we
also tested it with bigger source files. For this we used a Python file from
one of Chiels projects (this file is included as a smoke-test in the examples
package). We were able to parse this file correctly, though some desugaring
rules are still missing for other constructs that we did not support with the
old syntax as well.

## Analysis

Due to the time spent on the syntax and desugaring, the time we could spend on
the analysis was limited. We started by deciding on a structure of the scope
graph. We could just do a simple scope graph with one scope for each of the
scopes in the language. This would however limit the errors we could report.
Take for example the following code snippets and the warnings we would like to
show.

```python
if condition:
    b = 42
a = b            # Warning: b might not be declared
```

```python
b = 42
b = True         # Warning: assigning different types
```

```python
if condition:
    b = 42
b = True         # Warning: assigning different types
```

```python
if condition:
    b = 42
else:
    b = True     # Warning: assigning different types
```

This would not be possible with a simple scope graph as it requires
flow-analysis. The scope-structure we decided on is able to show a number of
those errors and could be expanded by an extra Stratego pass to show even
more. Its main idea is to create nested scopes for a code block. On each
assignment a new scope would be created and for dataflow statements two or
more. The scopes from the execution paths are then merged after the dataflow
statements finish. For the code snippet below, this results in the shown scope
graph.

```python
def func(a: int) -> int:
    if a < 10:
        b = 12
    else:
        a += 10
    return a

print(func(12))
```

![New syntax](img/M3-scope-graph.png)

In this image you can see a number of things:

* When declaring a variable, a reference is created to the superscope. This is
  done to check for previous declarations and type compatability.
* Else and Then branches of the if use the labels `E` and `T`. This is to make
  sure that there are not two equally good paths to declarations in earlier
  scopes. Now we can give one of the paths priority.

A limitation of using NaBL2 for this type of analysis is that we are currently
unable to handle the case of a variable declaration in two branches, where
both have a different type. We can get the set of variables on which this
would apply, but we cannot map over them to compare their types. In the future
this might be done by using an extra pass over the scope graph using Stratego.

## Codegen

The goal for the code generation in the third milestone was to enable printing
of both strings and integers, and increasing the possibilities for function
calls, such that mathematical functions such as factorial could be calculated
and performed. This goal was split into four sections,

* Functions and function calls
* Printing Integers
* Being able to determine the type of an expression and perform the correct
  string conversion
* Add strings to strings or integers to strings

### Functions

The example we had set for ourselves was to be able to compute a simple
recursive factorial function, something like,

```python
def factorial(value: int) -> int:
    if value <= 1:
        return 1
    return value * factorial(value-1)
```

To facilitate this and many more mathematical calculations, a large number of
comparison and binary operators were added to the code generation. Return
statements were also added.

### Printing Integers

In order to print integers, each character has to be calculated separately. To
achieve this, helper functions were added. The main idea is as follows, to
print the number `12345`, the following string must be created:

```
str = (12345 / 10 ** 4) % 10
 + (12345 / 10 ** 3) % 10
 + (12345 / 10 ** 2) % 10
 + (12345 / 10 ** 1) % 10
 + (12345 / 10 ** 0) % 10
```

Which can be generalized as,

```python
str = ""
for i in range(5):
  str += (n / 10 ** (len(n) - 1 - i)) % 10
```

Or expressed in webassembly (leaving the implementation for `$length` and
`$pow` out),

```wast
  (func $intRepr (param $value i32) (result i32)
    (local $digit i32)
    (local $location i32)
    (local $currLocation i32)
    (local $len i32)
    (set_local $len
      (call $length
        (get_local $value)
      )
    )
    (set_local $location
      (grow_memory
        (get_local $len)
      )
    )
    (set_local $digit
      (call $pow
        (i32.const 10)
        (i32.sub
          (get_local $len)
          (i32.const 1)
        )
      )
    )
    (set_local $currLocation
      (get_local $location)
    )
    (block
      (loop
        (i32.store
          (get_local $currLocation)
          (i32.add
            (i32.rem_s
              (i32.div_s
                (get_local $value)
                (get_local $digit)
              )
              (i32.const 10)
            )
            (i32.const 48)
          )
        )
        (set_local $currLocation
          (i32.add
            (get_local $currLocation)
            (i32.const 1)
          )
        )
        (set_local $digit
          (i32.div_s
            (get_local $digit)
            (i32.const 10)
          )
        )
        (br_if 1
          (i32.le_s
            (get_local $digit)
            (i32.const 0)
          )
        )
        (br 0)
      )
    )
    (get_local $location)
  )
```

Because WebAssembly does not support returning multiple values yet, the caller
is responsible for calculating the length of the resultant string separately.
This might be circumvented at a later point by a use of hidden global
variables.

### String Conversions

The analysis was mostly done, but not completely. Due to this fact it was
decided to assume all integer and function returns to be of integer type. This
done, rules were added for printing that check for types and convert them
accordingly.

Here follow the strategy for printing,

```javascript
  codegen-expr:
    (Call(Name(ID("print"), Load()), arg, kargs), mem) ->
      ([WastCall("$print", str)], mem1)
    where
      [type] := <map(get-type)> arg;
      (w-args, mem1) := <foldl(codegen-arg-fold)> (arg, ([], mem));
      str := <expr-to-string> (w-args, type)
```

### Concatenation

Having already created the type checks in the portion with string convertions,
adding the functionality for concatenating strings was reasonably simple, the
main differences were,

1.  There needed to be a check that at least one of the terms is a string
2.  The length needed to be added up from the length of the pieces

It ended up looking as follows,

```javascript
  codegen-expr:
    (BinOp(Add(), l, r), (a0, b0, c0, mptr0)) ->
      ([WastCall("$add_strings", <concat> [typed-w-l, typed-w-r]), lengths],
      (a2, b2, c2, mptr2))
    where
    l-type := <get-type> l;
    r-type := <get-type> r;
    (w-l, (a1, b1, c1, mptr1) ) := <codegen-expr> (l, (a0, b0, c0, mptr0));
    (w-r, (a2, b2, c2, mptr2)) := <codegen-expr> (r, (a1, b1, c1, mptr1));
    (<?StringT()> l-type <+ <?StringT()> r-type);
    typed-w-l := <if ?StringT() then !w-l else !<expr-to-string> (w-l, l-type) end> l-type;
    typed-w-r := <if ?StringT() then !w-r else !<expr-to-string> (w-r, r-type) end> r-type;
    l-length := <if ?StringT() then !WastI32Const(<int-to-string> <subt> (mptr1, mptr0)) else !<get-length> (w-l, l-type) end> l-type;
    r-length := <if ?StringT() then !WastI32Const(<int-to-string> <subt> (mptr2, mptr1)) else !<get-length> (w-r, r-type) end> r-type;
    lengths := WastI32Add(
      l-length,
      r-length)
```
