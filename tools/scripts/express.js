const express = require('express')
const app = express()
const port = 3000

app.get('/hello', (req, res) => {
  res.send("Hello!");
})

app.get('/hello/:name', (req, res) => {
  const name = req.params.name;
  console.log(`said hello to name: ${req.params.name}`);
  res.send(`Hello ${name}!`)
})

app.listen(port, () => {
  console.info(`Listening on port ${port}`)
})
