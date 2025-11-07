import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Build Arena - Elide vs Standard Toolchain',
  description: 'Live head-to-head comparison of build performance',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
