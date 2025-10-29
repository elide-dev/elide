import { Outlet, Link } from 'react-router-dom'

function App() {
  return (
    <div className="min-h-screen bg-black text-white">
      <div className="border-b border-gray-800 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <img src="/elide-logo.svg" alt="Elide" className="w-5 h-5" />
          <h1 className="text-lg font-medium">
            <Link to="/">Database Studio</Link>
          </h1>
        </div>
      </div>
      <Outlet />
    </div>
  )
}

export default App
