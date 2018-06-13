def factorial(n: int) -> int:
  if n <= 0:
    return 1
  return n * factorial(n-1)
n = 6
print("Factorial of:")
print(n)
print("Equals: ")
print(factorial(n))
