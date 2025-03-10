import polyglot

def say_hello(name = "Python"):
    return f"Hello from {name}"


polyglot.export_value("say_hello", say_hello)

