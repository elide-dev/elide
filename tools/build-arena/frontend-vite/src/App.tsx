import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import { HomePage } from './pages/HomePage'
import { TerminalTest } from './pages/TerminalTest'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/test/terminal" element={<TerminalTest />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
