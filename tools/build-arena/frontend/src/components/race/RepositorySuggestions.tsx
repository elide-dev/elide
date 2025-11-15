import { Repository } from '../../constants/repositories';

interface RepositorySuggestionsProps {
  suggestions: Repository[];
  onSelect: (url: string) => void;
}

export function RepositorySuggestions({ suggestions, onSelect }: RepositorySuggestionsProps) {
  return (
    <div className="mt-8 pt-6 border-t border-slate-700">
      <h3 className="text-lg font-semibold text-white mb-3">Suggested Repositories</h3>
      <p className="text-sm text-gray-400 mb-4">
        Try these popular Java projects to see Elide race against Maven/Gradle
      </p>
      <div className="space-y-2">
        {suggestions.map((repo, index) => (
          <button
            key={index}
            onClick={() => onSelect(repo.url)}
            className="block w-full text-left px-4 py-3 bg-slate-700 hover:bg-slate-600 text-gray-300 rounded-lg transition-colors group"
          >
            <div className="flex items-center justify-between">
              <span className="font-medium group-hover:text-white transition-colors">
                {repo.name}
              </span>
              <span className="text-xs text-gray-500 bg-slate-800 px-2 py-1 rounded">
                ~{repo.time}
              </span>
            </div>
          </button>
        ))}
      </div>
      <p className="mt-4 text-xs text-gray-500 text-center">
        Refresh page for different suggestions • 18 popular projects available
      </p>
    </div>
  );
}
