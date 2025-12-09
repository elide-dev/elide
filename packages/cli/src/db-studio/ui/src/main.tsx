import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.tsx'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import Home from './routes/Home.tsx'
import Database from './routes/Database.tsx'
import Table from './routes/Table.tsx'
import Query from './routes/Query.tsx'
import { TableEditor } from './routes/TableEditor.tsx'

const queryClient = new QueryClient()

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Home /> },
      {
        path: 'database/:dbId',
        element: <Database />,
        children: [
          {
            path: 'tables',
            element: (
              <div className="flex-1 p-6 overflow-auto flex items-center justify-center text-muted-foreground">
                Select a table to view data
              </div>
            ),
          },
          { path: 'table/:tableName', element: <Table /> },
          { path: 'table/:tableName/edit', element: <TableEditor /> },
          { path: 'query', element: <Query /> },
          { path: 'tables/new', element: <TableEditor /> },
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
  </StrictMode>
)
