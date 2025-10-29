import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.tsx'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import Databases from './routes/Databases.tsx'
import Database from './routes/Database.tsx'
import Table from './routes/Table.tsx'

const queryClient = new QueryClient()

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Databases /> },
      {
        path: 'database/:dbIndex',
        element: <Database />,
        children: [
          { path: 'tables', element: <div className="flex-1 p-6 overflow-auto flex items-center justify-center text-gray-500">Select a table to view data</div> },
          { path: 'table/:tableName', element: <Table /> },
        ],
      },
    ],
  },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
