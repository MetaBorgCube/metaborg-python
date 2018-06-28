def factorial(n) -> int:
  if n < 1: 
    return 1
  return n * factorial(n-1)
phrases = ["I like Football more!", "I Like Hockey "]
age = factorial(4)
phrases[1] = phrases[1] + "a lot!"
print("Rasmus, " + age + " years old, said: " + phrases[1])
print("Chiel, " + (factorial(3) + 16) + " years old, said: " + phrases[0])