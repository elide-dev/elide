import fibgen from "./fibgen.py"

function doFibgen() {
  fibgen.fib_as_list().forEach(i => console.log("fib", i))
}

doFibgen()
