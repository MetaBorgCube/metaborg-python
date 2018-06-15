# Milestone 3

In the third milestone the focus was mainly on increasing the coverage of the
language. It was expected that development on the Webassembly backend would be
slow, therefore it was deemed very likely that the code analysis would achieve
a higher coverage. In the following sections our progress is explained for the
three main areas of the code.

## Syntax/Desugaring

## Analysis

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
