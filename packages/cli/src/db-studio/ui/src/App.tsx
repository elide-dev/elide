import { Outlet, Link, useLocation } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { ArrowLeft } from 'lucide-react'

function App() {
  const location = useLocation()
  const isHomeScreen = location.pathname === '/'
  return (
    <div className="min-h-screen bg-black text-white">
      <div className="border-b border-gray-800 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
        <Link to="/">
          <div className="flex items-center gap-2">
            <img src="/elide-logo.svg" alt="Elide" className="w-8 h-8" />
            <h1 className="text-lg font-medium">
              Database Studio
            </h1>
          </div>
          </Link>
        </div>
        {!isHomeScreen && (
          <div>
            <Button asChild variant="outline" size="sm" className="border-gray-800 bg-gray-950 text-gray-200 hover:bg-gray-900 hover:text-white">
              <Link to="/" aria-label="Back to databases">
                <ArrowLeft className="w-4 h-4" />
                <span>Back to databases</span>
              </Link>
            </Button>
          </div>
        )}
      </div>
      <Outlet />
    </div>
  )
}

export default App
