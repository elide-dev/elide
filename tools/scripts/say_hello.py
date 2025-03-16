default_name = "Python"

def say_hello(name = default_name):
    return f"Hello from {name}"

def goodbye(name = default_name):
  return f"Goodbye from {name}"

def message(leaving = False):
  if leaving:
    return goodbye
  return say_hello
