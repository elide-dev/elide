package dev.elide.buildtools.gradle.plugin.tasks

import com.google.common.base.Preconditions.checkState
import com.google.common.collect.AbstractIterator
import com.google.common.graph.Graph
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.stream.Collectors
import java.util.stream.Stream

// Implementation of topological sort on top of a Guava graph.
@Suppress("UnstableApiUsage")
internal class TopologicalGraphIterator<N> constructor (private val graph: Graph<N>) : AbstractIterator<N>() {
    private val roots: Queue<N> = graph
        .nodes()
        .stream()
        .filter { node -> graph.inDegree(node) == 0 }
        .collect(Collectors.toCollection { ConcurrentLinkedQueue() })

    private val nonRootsToInDegree: MutableMap<N, Int> = graph
        .nodes()
        .stream()
        .filter { node -> graph.inDegree(node) > 0 }
        .collect(Collectors.toMap({ node -> node }, graph::inDegree, { a, _ -> a }, {
            ConcurrentSkipListMap()
        }))

    override fun computeNext(): N? {
        // implements Kahn's algorithm, for more see here:
        // https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm
        if (!roots.isEmpty()) {
            val next: N = roots.remove()
            for (successor: N in graph.successors(next)) {
                val newInDegree: Int = nonRootsToInDegree[successor]!! - 1
                nonRootsToInDegree[successor] = newInDegree
                if (newInDegree == 0) {
                    nonRootsToInDegree.remove(successor)
                    roots.add(successor)
                }
            }
            return next
        }
        checkState(
            nonRootsToInDegree.isEmpty(),
            "failure: graph has a cycle"
        )
        return endOfData()
    }

    companion object {
        /** @return Map [op] across each node in the provided [graph], in reverse topological order. */
        @JvmStatic fun <N, R> map(graph: Graph<N>, op: (N) -> R): Stream<R> {
            val iterator = TopologicalGraphIterator(graph)
            val nodes = iterator.asSequence().toList()
            return nodes.reversed().stream().map {
                op.invoke(it)
            }
        }
    }
}
