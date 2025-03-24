import json

def emit_json():
    return json.dumps([1, 2, 3])

if __name__ == "__main__":
    print(emit_json())

