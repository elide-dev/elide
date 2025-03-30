import { params, huggingface, infer } from "elide:llm"

const prompt = "Complete the sentence: The quick brown fox jumped over the"

const llama7b = huggingface({
  repo: "TheBloke/Llama-2-7B-Chat-GGUF",
  name: "llama-2-7b-chat.Q4_K_M.gguf",
})

console.log(`Prompt: ${prompt}`)
const op = infer(params(), llama7b, prompt)
console.log(`Operation: ${op}`)
// @ts-ignore
const result = await op
console.log(`Answer: ${result}`)
console.log("Done.")
