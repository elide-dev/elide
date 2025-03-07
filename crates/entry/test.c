 #include <stdio.h>
 #include <stdlib.h>

 #include "libentry.h"

 int main(int argc, char **argv) {
   if (argc != 2) {
       fprintf(stderr, "Usage: %s <filter>\n", argv[0]);
       exit(1);
   }

   graal_isolate_t *isolate = NULL;
   graal_isolatethread_t *thread = NULL;

   if (graal_create_isolate(NULL, &isolate, &thread) != 0) {
       fprintf(stderr, "initialization error\n");
       return 1;
   }

   printf("Init result: %d\n", elide_entry_init(thread));

   int run_result = elide_entry_run(thread);
   printf("Run result: %d\n", run_result);

   graal_tear_down_isolate(thread);
   printf("done");
}
