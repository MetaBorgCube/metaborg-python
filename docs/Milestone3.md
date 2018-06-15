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

### Concatenation
