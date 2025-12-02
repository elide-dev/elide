interface HomeHeaderProps {
  databaseCount: number
}

export function HomeHeader({ databaseCount }: HomeHeaderProps) {
  return (
    <div className="flex flex-col items-center mb-12">
      <img src="/elide-logo.svg" alt="Elide" className="w-16 h-16 mb-6" />
      <h2 className="text-4xl font-semibold mb-3">Elide Database Studio</h2>
      <p className="text-muted-foreground mb-2">Select a database to inspect</p>
      <p className="text-sm text-muted-foreground">{databaseCount} databases found</p>
    </div>
  )
}
