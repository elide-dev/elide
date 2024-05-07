# Prints the arguments handed to it from the command line.
import sys

def main():
    print("Executable: %s" % sys.executable)
    print("Arg count: %s" % str(len(sys.argv)))
    for i in range(len(sys.argv)):
        print("Argument", i, ":", sys.argv[i])

if __name__ == "__main__":
    main()
