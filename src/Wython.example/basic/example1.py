def factorial(n: int) -> int:
  if n <= 0:
    return 1
  return n * factorial(n-1)
def printFactorial(n: int):
  print("The factorial of " + n + " is:  " + factorial(n))
printFactorial(3*2)
print(2 + 2)
def a():
  print("a")
  print("b")
if True:
  print("True!")
if False:
  print("False!")
print("c")