import {z} from "zod";

const schema = z.object({
  name: z.string(),
  age: z.number(),
});

const data = {
  name: "John",
  age: 30,
};

const result = schema.parse(data);
console.log(result);