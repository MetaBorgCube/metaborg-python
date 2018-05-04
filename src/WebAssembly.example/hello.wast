(module
  (func $_log (import "console" "log") )
  (func $_logString (import "lib" "logString") (param i32 i32))
  (memory $_memory (import "memory" "memory") 1)
  (data (i32.const 0) "Hello, Rasmus!\n")
  (data (i32.const 15) "Hello, Chiel!\n")

  (func $main 
  )
  (export "main" (func $main))
)
