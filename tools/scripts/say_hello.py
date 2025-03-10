from elide import bind, poly

default_name = "Python"

@bind
def say_hello(name = default_name):
    return f"Hello from {name}"

@poly(name = "say_goodbye")
def goodbye(name = default_name):
  return f"Goodbye from {name}"

@bind
def message(leaving = False):
  if leaving:
    return goodbye
  return say_hello
