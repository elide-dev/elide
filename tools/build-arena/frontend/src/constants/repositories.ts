export interface Repository {
  name: string;
  url: string;
  time: string;
}

export interface RepositoryCategories {
  small: Repository[];
  medium: Repository[];
  large: Repository[];
}

// Popular Java repositories categorized by build size
export const EXAMPLE_REPOS: RepositoryCategories = {
  small: [
    { name: 'Spring PetClinic', url: 'https://github.com/spring-projects/spring-petclinic.git', time: '<1 min' },
    { name: 'Apache Commons Lang', url: 'https://github.com/apache/commons-lang.git', time: '<1 min' },
    { name: 'Apache Commons IO', url: 'https://github.com/apache/commons-io.git', time: '<1 min' },
    { name: 'Google Gson', url: 'https://github.com/google/gson.git', time: '<1 min' },
    { name: 'Square Okio', url: 'https://github.com/square/okio.git', time: '<1 min' },
    { name: 'Joda Time', url: 'https://github.com/JodaOrg/joda-time.git', time: '<1 min' },
  ],
  medium: [
    { name: 'Google Guava', url: 'https://github.com/google/guava.git', time: '1-5 min' },
    { name: 'Square OkHttp', url: 'https://github.com/square/okhttp.git', time: '1-5 min' },
    { name: 'Square Retrofit', url: 'https://github.com/square/retrofit.git', time: '1-5 min' },
    { name: 'JUnit 5', url: 'https://github.com/junit-team/junit5.git', time: '1-5 min' },
    { name: 'Mockito', url: 'https://github.com/mockito/mockito.git', time: '1-5 min' },
    { name: 'Google Guice', url: 'https://github.com/google/guice.git', time: '1-5 min' },
    { name: 'Jackson Core', url: 'https://github.com/FasterXML/jackson-core.git', time: '1-5 min' },
  ],
  large: [
    { name: 'Spring Boot', url: 'https://github.com/spring-projects/spring-boot.git', time: '5-15 min' },
    { name: 'Spring Framework', url: 'https://github.com/spring-projects/spring-framework.git', time: '5-15 min' },
    { name: 'Apache Kafka', url: 'https://github.com/apache/kafka.git', time: '5-15 min' },
    { name: 'Elasticsearch', url: 'https://github.com/elastic/elasticsearch.git', time: '5-15 min' },
    { name: 'Gradle', url: 'https://github.com/gradle/gradle.git', time: '5-15 min' },
  ],
};

/**
 * Pick random repositories from each category for suggestions
 */
export function pickRandomRepos(): Repository[] {
  const pickRandom = <T,>(arr: T[], count: number): T[] => {
    const shuffled = [...arr].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, count);
  };

  return [
    ...pickRandom(EXAMPLE_REPOS.small, 2),
    ...pickRandom(EXAMPLE_REPOS.medium, 2),
    ...pickRandom(EXAMPLE_REPOS.large, 1),
  ];
}
