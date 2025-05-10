export const message = "Hello"

export function render(salutation: string = "World"): string {
  return `${salutation}, ${message}!`
}
