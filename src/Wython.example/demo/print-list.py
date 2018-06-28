def listString(l) -> str:
  i = 0
  s = "["
  while(i < len(l)):
    if i == (len(l) - 1):
      s = s + l[i]
    else: 
      s = s + l[i] + ", "
    i = i + 1
  return s + "]"
l = [1, 5, "TU Delft", 2]
print(listString(l))