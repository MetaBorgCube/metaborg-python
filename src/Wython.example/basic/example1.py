def factorial(n) -> int:
  if n < 1: 
    return 1
  return n * factorial(n-1)
phrases = [2, 4, "I Like Hockey "]
age = factorial(4)
phrases[2] = phrases[2] + "a lot!"
print("Rasmus, " + age + " years old, said: " + phrases[2])
