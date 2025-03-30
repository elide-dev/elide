import { params, huggingface, inferSync } from "elide:llm"

const prompt = "Complete the sentence: The quick brown fox jumped over the"

const llama7b = huggingface({
  repo: "TheBloke/Llama-2-7B-Chat-GGUF",
  name: "llama-2-7b-chat.Q4_K_M.gguf",
})

const result = inferSync(params(), llama7b, prompt)

console.log(`Prompt: ${prompt}`)
console.log(`Answer: ${result}`)
console.log("Done.")
