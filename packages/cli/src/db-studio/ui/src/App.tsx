import { Outlet } from 'react-router-dom'

function App() {
  return (
    <div className="min-h-screen bg-black text-white">
      <Outlet />
    </div>
  )
}

export default App
