/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
@file:Suppress("NothingToInline")

package elide.struct

import kotlin.jvm.JvmField
import kotlin.jvm.Transient
import elide.struct.RedBlackTree.NodeColor.BLACK
import elide.struct.RedBlackTree.NodeColor.RED

/**
 * Implementation of a Red/Black Tree data structure, serving as a base for specialized data structures such as maps
 * and sets.
 *
 * This class is _not_ safe for concurrent modifications, no locks are used and adding/removing/updating values while
 * iterating will cause unspecified behavior.
 */
public abstract class RedBlackTree<K : Comparable<K>, V> internal constructor() {
  /** The color of a node in a Red/Black tree. */
  protected enum class NodeColor { RED, BLACK }

  /**
   * Represents a node in a Red/Black Tree and its [color], identified by a [key] and holding a [value] which is used
   * to provide map-like features. Each node has an optional [parent], and [left] and [right] child nodes. If the
   * [parent] is null, then the node must be the root of the tree.
   *
   * Nodes obey certain rules according to their color:
   *
   * - The root node is always [BLACK].
   * - Leaf nodes are [BLACK].
   * - A [RED] node can only have [BLACK] children.
   *
   * It is recommended to use the provided inline extensions  when manipulating nodes, as they can easily and
   * efficiently query/update a node's relatives and other relevant properties.
   */
  protected class Node<K : Comparable<K>, V>(
    /** A unique key identifying this node in the tree. */
    @Volatile override var key: K,
    /** The value associated with the [key]. */
    @Volatile override var value: V,
    /** The color of this node. */
    @Volatile @JvmField public var color: NodeColor,
    /** The left child of this node. */
    @Volatile @JvmField public var left: Node<K, V>? = null,
    /** The right child of this node. */
    @Volatile @JvmField public var right: Node<K, V>? = null,
    /** The parent of this node, or `null` if this node is the root. */
    @Volatile @JvmField public var parent: Node<K, V>? = null,
  ) : MutableMap.MutableEntry<K, V> {
    override fun setValue(newValue: V): V {
      val old = value
      value = newValue
      return old
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Node<*, *>) return false
      if (key != other.key) return false
      if (value != other.value) return false
      return true
    }

    override fun hashCode(): Int {
      var result = key.hashCode()
      result = 31 * result + value.hashCode()
      return result
    }
  }

  //---------------------------------------------------------------------------
  // node helper extensions
  //---------------------------------------------------------------------------

  /**
   * Returns `true` if this node is not `null` and its color is [RED]. Empty (null) nodes are considered [BLACK] by
   * default in a Red/Black Tree.
   */
  private inline val Node<K, V>?.isRed: Boolean
    get() = this != null && color == RED

  /**
   * Returns `true` if this node is `null` or its color is [BLACK]. Empty (null) nodes are considered [BLACK] by
   * default in a Red/Black Tree.
   */
  private inline val Node<K, V>?.isBlack: Boolean
    get() = this == null || color == BLACK

  /**
   * Returns a boolean value representing the subtree this node is located in, relative to its parent: `true` if it is
   * the right child, `false` if it is the left child.
   *
   * If there is no parent (i.e. the node is the root), an [IllegalStateException] is thrown.
   */
  private inline val Node<K, V>.direction: Boolean
    get() = (parent ?: error("Node has no parent, cannot compute direction")).right == this

  /**
   * Returns a node's sibling if it exists. If this node is a left-hand child, the right-hand child of the parent (if
   * any) is selected, and vice-versa. If the node has no parent (i.e. it is the tree root), `null` is returned.
   */
  private inline val Node<K, V>.sibling: Node<K, V>?
    get() = parent?.child(!direction)

  /**
   * Returns the child of this node corresponding to a [direction] (right child if `true`, left child if `false`), if
   * it exists.
   */
  private fun Node<K, V>.child(direction: Boolean): Node<K, V>? {
    return if (direction) right else left
  }

  /**
   * Sets the a [child] of this node in the corresponding [direction] (right child if `true`, left child if `false`),
   * and automatically sets the `parent` of the [child] to this node, if it is not `null`.
   *
   * Note that if the corresponding child is already assigned to a node, it will be replaced but its parent field will
   * not be updated. It is up to the caller to correctly reparent the orphaned node.
   */
  private fun Node<K, V>.setChild(direction: Boolean, child: Node<K, V>?) {
    if (direction) right = child
    else left = child

    child?.parent = this
  }

  /** Remove a [child] node, failing with an exception if it is not actually a child of this node. */
  private fun Node<K, V>.remove(child: Node<K, V>) = when (child) {
    right -> right = null
    left -> left = null
    else -> error("The specified node ($child) is not a child of this node ($this)")
  }

  /** Replace a [child] node with another, failing with an exception if [child] is not a child of this node. */
  private fun Node<K, V>.replace(child: Node<K, V>, with: Node<K, V>) = when (child) {
    right -> right = with.also { it.parent = this }
    left -> left = with.also { it.parent = this }
    else -> error("The specified node ($child) is not a child of this node ($this)")
  }

  /** Swap the `key` and `value` of this node and another, preserving all parents, children, and colors. */
  private fun Node<K, V>.swap(other: Node<K, V>) {
    val oldKey = key
    val oldValue = value

    key = other.key
    value = other.value

    other.key = oldKey
    other.value = oldValue
  }

  //---------------------------------------------------------------------------
  // private fields
  //---------------------------------------------------------------------------

  /** Private size field indicating the total number of nodes in the tree. */
  @Transient @Volatile protected var nodeCount: Int = 0
    private set

  /**
   * The root node of the tree, `null` when there are no entries in the map. According to Red/Black Tree rules, the
   * root node must always be black.
   *
   * Setting the value of this property to a non-null node will set the node's color to [BLACK], as well as reset its
   * parent to `null`. The [nodeCount] will also be set to `1` if it was previously `0`.
   *
   * If a `null` value is set, the [nodeCount] will also be set to `0`.
   */
  @Transient private var root: Node<K, V>? = null
    set(value) = if (value != null) {
      if (nodeCount == 0) nodeCount++

      field = value
      value.parent = null
      value.color = BLACK
    } else {
      nodeCount = 0
      field = null
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Map<*, *>) return false
    if (nodeCount != other.size) return false

    return nodes().all { other[it.key] == it.value }
  }

  override fun hashCode(): Int {
    var result = root?.hashCode() ?: 0
    result = 31 * result + nodeCount
    result = 31 * result + nodes().sumOf { it.hashCode() }
    return result
  }

  //---------------------------------------------------------------------------
  // map helpers
  //---------------------------------------------------------------------------

  /** Find a [Node] given its [key] in O(log n) time, or return `null` if no such node exists. */
  protected fun findNodeByKey(key: K): Node<K, V>? {
    var node = root

    while (node != null) {
      val comparison = key.compareTo(node.key)

      if (comparison == 0) return node
      else if (comparison < 0) node = node.left
      else if (comparison > 0) node = node.right
    }

    return null
  }

  /**
   * Remove the node with the given [key] and return it, or return `null` if no such node exists. This method uses
   * [findNodeByKey] to run in O(log n) time.
   */
  protected fun removeNodeByKey(key: K): Node<K, V>? {
    // find the node with the given key and remove it
    return findNodeByKey(key)?.let(::removeNode)
  }

  /**
   * Returns a [Sequence] of all nodes in the tree, in ascending order of their keys. Updating structural properties of
   * the nodes (parent or children) during iteration is _not supported_ and will cause undefined behavior.
   */
  protected fun nodes(): Sequence<Node<K, V>> {
    val root = this.root ?: return emptySequence()

    suspend fun SequenceScope<Node<K, V>>.walk(node: Node<K, V>) {
      node.left?.let { walk(it) }
      yield(node)
      node.right?.let { walk(it) }
    }

    return sequence { walk(root) }
  }

  /** Clear the entire tree, detaching the root and resetting the node count. */
  protected fun reset() {
    root = null
  }

  //---------------------------------------------------------------------------
  // update operations
  //---------------------------------------------------------------------------

  /**
   * Insert a node with the given [key] and [value] into the tree. If a node already exists with the same key, its value
   * will be replaced. The value previously associated with the node (if any) is returned.
   *
   * If a new node is inserted, and it violates the tree constraints, it will trigger a [rebalance][rebalanceAdded] to
   * restore the Red/Black properties.
   */
  protected fun addNode(key: K, value: V): V? {
    var node: Node<K, V> = root ?: run {
      // if there is no root on the tree (it's empty), install it
      root = Node(key, value, color = BLACK)
      return null
    }

    while (true) {
      // compare keys, on a match, swap the value and return the old one
      val comparison = key.compareTo(node.key)
      if (comparison == 0) return node.value.also { node.value = value }

      // select a subtree, right if the key is larger, left otherwise;
      // if the chosen subtree is empty, insert the node and rebalance
      val direction = comparison > 0
      node = node.child(direction) ?: Node(key, value, color = RED).let { new ->
        node.setChild(direction, new)
        rebalanceAdded(new)

        nodeCount++
        return null
      }
    }
  }

  /**
   * Rebalance a [node] and its ancestors if necessary to restore the properties of the Red/Black tree after a new node
   * is inserted. This method may cause several nodes to be rotated and/or recolored, so it should only be called when
   * the tree state is locked.
   */
  private tailrec fun rebalanceAdded(node: Node<K, V>) {
    // (1) if the node is the root, it can only be black
    val parent = node.parent ?: return

    // (2) if the parent is black, color it red and return
    if (parent.isBlack) {
      node.color = RED
      return
    }

    // at this point, the parent is red (2); tree properties ensure there is a grandparent
    val grandparent = parent.parent!!

    // (3) if the uncle also red, color it and the parent black, and the grandparent red
    parent.sibling?.takeIf { it.isRed }?.let { uncle ->
      parent.color = BLACK
      uncle.color = BLACK

      grandparent.color = RED
      return rebalanceAdded(grandparent)
    }

    // if the parent is red, and the uncle is black, rotations are needed
    val parentDirection = parent.direction

    // if directions don't match, an extra rotation is needed (left-right/right-left)
    // in the direction of the parent; the node itself is also recolored to black
    if (parentDirection != node.direction) {
      rotate(parent, parentDirection)
      node.color = BLACK
    } else {
      // if the directions match (left-left/right-right), set the parent's color instead
      parent.color = BLACK
    }

    // all four cases rotate the grandparent away from the parent and color it red
    rotate(grandparent, !parentDirection)
    grandparent.color = RED
  }

  /**
   * Remove a [node] from the tree and return it. The node _must_ be part of the tree, otherwise this method's behavior
   * is undefined. Additional [balancing][rebalanceRemoved] may be required if the node is not a leaf.
   */
  protected fun removeNode(node: Node<K, V>): Node<K, V> {
    // find the replacement for the removed node
    val parent = node.parent
    val leftChild = node.left
    val rightChild = node.right

    // if the node has no children, remove it and return
    if (leftChild == null && rightChild == null) {
      // if node is root, clear the tree
      if (parent == null) return node.also { root = null }

      // if the node is black, we need to rebalance
      if (node.isBlack) rebalanceRemoved(node)
      else node.sibling?.let { it.color = RED }

      parent.remove(node)
      nodeCount--
      return node
    }

    // node has a single child, replace node with child
    if ((leftChild != null) xor (rightChild != null)) (leftChild ?: rightChild)?.let { successor ->
      if (parent == null) root = successor
      else parent.replace(node, with = successor)

      // if both nodes are black, we need to rebalance
      if (node.isBlack && successor.isBlack) rebalanceRemoved(successor)
      else successor.color = BLACK

      nodeCount--
      return node
    }

    // node has two children, replace it with the in-order successor, then remove the successor
    var successor = rightChild!!
    while (true) successor = successor.left ?: break
    node.swap(successor)

    if (successor.isBlack) rebalanceRemoved(successor)
    else successor.sibling?.let { it.color = RED }

    successor.parent?.remove(successor)
    nodeCount--

    return node
  }

  /**
   * Rebalance a [node] and its ancestors if necessary to restore the properties of the Red/Black tree after a node is
   * removed. This method may cause several nodes to be rotated and/or recolored, so it should only be called when the
   * tree state is locked.
   */
  private tailrec fun rebalanceRemoved(node: Node<K, V>) {
    val parent = node.parent ?: return

    // if there is no sibling, push double-black up
    val sibling = node.sibling ?: return rebalanceRemoved(parent)

    if (sibling.isRed) {
      // red sibling: recolor parent and sibling, rotate parent, then retry
      parent.color = RED
      sibling.color = BLACK
      rotate(parent, !sibling.direction)

      rebalanceRemoved(node)
      return
    }

    if (sibling.left.isBlack && sibling.right.isBlack) {
      // black sibling with all-black children, recolor and recurse
      sibling.color = RED

      if (parent.isRed) parent.color = BLACK
      else rebalanceRemoved(parent)

      return
    }

    // sibling has at least one red child, find it
    val siblingDirection = sibling.direction
    val redDirection = (sibling.right.isRed)

    if (siblingDirection == redDirection) {
      // left-left/right-right
      sibling.color = parent.color
      sibling.child(redDirection)!!.color = sibling.color
      rotate(parent, !siblingDirection)
    } else {
      // right-left/left-right
      sibling.child(redDirection)!!.color = parent.color
      rotate(sibling, siblingDirection)
      rotate(parent, !siblingDirection)
    }

    parent.color = BLACK
  }

  /**
   * Rotate a [node] in the given [direction] (to the right if `true`, to the left if `false`). The [node] _must_ have
   * a child in the direction opposing the rotation (a left child for right-rotation, right child for left-rotation),
   * which is used as the "pivot" for the operation, otherwise an exception will be thrown.
   *
   * Rotation affects the node's parent and its "pivot" child, the following figure visually depicts left-rotation
   * (right-rotation is a mirror case and has the same result, only in the opposite direction):
   *
   * ```
   *  initial  | reparent N, R | reparent <CR> | reparent [N] (final)
   * ----------|---------------|---------------|---------------------
   *     /     |            /  |            /  |         /
   *   [N]     |   [N]    (R)  |   [N]    (R)  |       (R)
   *   / \     |   /      /    |   / \         |       /
   *  L  (R)   |  L     <CR>   |  L <CR>       |     [N]
   *     /     |               |               |     / \
   *   <CR>    |               |               |    L <CR>
   * ----------|------(1)------|------(2)------|-----(3)-------------
   * ↑ rotate [N] to the left
   * ```
   *
   * > See this method's source code for implementation details on (1), (2), and (3).
   *
   * Rotation is used to maintain the properties of the Red/Black Tree after a node is [inserted][rebalanceAdded] or
   * [removed][rebalanceRemoved], and can be paired with recoloring when necessary.
   */
  private fun rotate(node: Node<K, V>, direction: Boolean) {
    val pivot = node.child(!direction) ?: error("node is missing a ${if (direction) "left" else "right"} child")
    val orphan = pivot.child(direction)

    node.parent?.replace(node, pivot) // (1)
    if (node == root) root = pivot

    node.setChild(!direction, orphan) // (2)
    pivot.setChild(direction, node) // (3)
  }
}
