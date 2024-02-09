/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.struct

import kotlin.concurrent.Volatile
import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import kotlin.jvm.Transient
import elide.struct.api.SortedMap

/**
 *
 */
public abstract class AbstractTreeMap<Key, Value> @Suppress("UNUSED_PARAMETER") protected constructor (
  pairs: Iterable<Pair<Key, Value>>,
  presorted: Boolean = false,
) : SortedMap<Key, Value> where Key : Comparable<Key> {
  /** */
  internal companion object {
    /**
     *
     */
    internal val RED: NodeColor = NodeColor.RED

    /**
     *
     */
    internal val BLACK: NodeColor = NodeColor.BLACK
  }

  /**
   *
   */
  internal enum class NodeColor {
    RED,
    BLACK
  }

  /**
   *
   */
  internal class TreeNode<Key: Comparable<Key>, Value>(
    /**
     *
     */
    @Volatile @JvmField var key: Key,

    /**
     *
     */
    @Volatile @JvmField var value: Value,

    /**
     *
     */
    @Volatile @JvmField var left: TreeNode<Key, Value>? = null,

    /**
     *
     */
    @Volatile @JvmField var right: TreeNode<Key, Value>? = null,

    /**
     *
     */
    @Volatile @JvmField var parent: TreeNode<Key, Value>? = null,

    /**
     *
     */
    @Volatile @JvmField var color: NodeColor = BLACK,
  ) {
    internal companion object {
      fun <Key: Comparable<Key>, Value> of(pair: Pair<Key, Value>): TreeNode<Key, Value> = TreeNode(
        pair.first,
        pair.second
      )
    }

    internal fun child(direction: Boolean): TreeNode<Key, Value>? = if (direction) right else left

    internal fun setLeft(child: TreeNode<Key, Value>?) {
      left = child
      child?.parent = this
    }

    internal fun setRight(child: TreeNode<Key, Value>?) {
      right = child
      child?.parent = this
    }

    override fun toString(): String {
      return "Node(key=$key, value=$value, color=${if (color == RED) "RED" else "BLACK"})"
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is TreeNode<*, *>) return false
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

  /**
   *
   */
  @JvmInline public value class Entry<Key, Value> private constructor (
    private val node: TreeNode<Key, Value>
  ) : Map.Entry<Key, Value>, Comparable<Key> where Key : Comparable<Key> {
    override val key: Key get() = node.key
    override val value: Value get() = node.value
    override fun compareTo(other: Key): Int = key.compareTo(other)

    /** */
    public companion object {
      /**
       *
       */
      @JvmStatic public fun <Key: Comparable<Key>, Value> of(pair: Pair<Key, Value>): Entry<Key, Value> = Entry(
        TreeNode.of(pair)
      )

      /**
       *
       */
      @JvmStatic public fun <Key: Comparable<Key>, Value> of(key: Key, value: Value): Entry<Key, Value> = Entry(
        TreeNode.of(key to value)
      )

      /**
       *
       */
      @JvmStatic internal fun <Key: Comparable<Key>, Value> of(node: TreeNode<Key, Value>): Entry<Key, Value> = Entry(
        node
      )

      /**
       *
       */
      @JvmStatic public fun <Key: Comparable<Key>, Value> mutable(pair: Pair<Key, Value>): MutableEntry<Key, Value> =
        MutableEntry(TreeNode.of(pair))

      /**
       *
       */
      @JvmStatic internal fun <Key: Comparable<Key>, Value> mutable(
        node: TreeNode<Key, Value>
      ): MutableEntry<Key, Value> = MutableEntry(node)
    }
  }

  /**
   *
   */
  @JvmInline public value class MutableEntry<Key, Value> internal constructor (
    private val node: TreeNode<Key, Value>
  ) : MutableMap.MutableEntry<Key, Value>, Comparable<Key> where Key : Comparable<Key> {
    override val key: Key get() = node.key
    override val value: Value get() = node.value
    override fun compareTo(other: Key): Int = key.compareTo(other)

    override fun setValue(newValue: Value): Value {
      val oldValue = node.value
      node.value = newValue
      return oldValue
    }
  }

  /**
   *
   */
  @Transient private var root: TreeNode<Key, Value>? = null

  //
  @Transient @Volatile private var innerSize: UInt = 0u

  init {
    // skip first; it's the root
    pairs.forEach { addNode(it.first, it.second) }
  }

  /**
   *
   */
  internal fun applyRoot(root: TreeNode<Key, Value>) {
    root.parent = null
    root.color = BLACK
    this.root = root
    if (innerSize == 0u) {
      innerSize++
    }
  }

  /**
   *
   */
  private fun <R> withRoot(op: (TreeNode<Key, Value>?) -> R): R = op(root)

  /**
   *
   */
  protected fun addNode(key: Key, value: Value): Value? = withRoot { root ->
    // if there is no root on the tree (it's empty), install it
    if (root == null) {
      applyRoot(TreeNode.of(key to value).apply {
        color = BLACK
      })
      return@withRoot value
    }

    // find the insertion point and add the new node.
    fun walkTree(): Pair<TreeNode<Key, Value>?, Value?> {
      var current: TreeNode<Key, Value> = root
      while (true) {
        val comparison = key.compareTo(current.key)
        current = when {
          comparison < 0 -> when (val left = current.left) {
            null -> TreeNode.of(key to value).let { node ->
              current.setLeft(node)
              innerSize++
              return node to value
            }
            else -> left
          }
          comparison > 0 -> when (val right = current.right) {
            null -> TreeNode.of(key to value).let { node ->
              current.setRight(node)
              innerSize++
              return node to value
            }
            else -> right
          }
          else -> current.value.let { oldValue ->
            current.value = value
            return current to oldValue
          }
        }
      }
    }
    walkTree().also { pair ->
      pair.first?.let { node -> maybeRebalance(node) }
    }.second
  }

  //
  private fun maybeRebalance(node: TreeNode<Key, Value>) {
    when (val parent = node.parent) {
      // if the node is the root, color it black and return
      null -> {
        node.color = BLACK
        return
      }

      // if the parent is black, color it red and return
      else -> if (parent.color == BLACK) {
        node.color = RED
        return
      } else {
        // if the parent is red, and the uncle is red, recolor the parent and uncle to black, and the grandparent to red
        val uncle = node.parent?.parent?.child(node.parent == node.parent?.parent?.right)
        if (uncle?.color == RED) {
          node.parent?.color = BLACK
          uncle.color = BLACK
          node.parent?.parent?.color = RED
          node.parent?.parent?.let {
            maybeRebalance(it)
          }
          return
        }

        // if the parent is red, and the uncle is black, and the node is the right child of the parent, rotate left
        if (node == node.parent?.right && node.parent == node.parent?.parent?.left) {
          node.parent?.let { rotateLeft(it) }
          node.left?.let { maybeRebalance(it) }
          return
        }

        // if the parent is red, and the uncle is black, and the node is the left child of the parent, rotate right
        if (node == node.parent?.left && node.parent == node.parent?.parent?.right) {
          node.parent?.let { rotateRight(it) }
          node.right?.let { maybeRebalance(it) }
          return
        }
      }
    }
  }

  //
  private fun rotateLeft(node: TreeNode<Key, Value>) {
    val pivot = node.right ?: return
    node.setRight(pivot.left)
    pivot.setLeft(node)
    pivot.parent = node.parent
    node.parent = pivot
    pivot.parent?.child(node == pivot.parent?.left)?.let { direction ->
      pivot.parent?.setLeft(pivot)
    }
  }

  //
  private fun rotateRight(node: TreeNode<Key, Value>) {
    val pivot = node.left ?: return
    node.setLeft(pivot.right)
    pivot.setRight(node)
    pivot.parent = node.parent
    node.parent = pivot
    pivot.parent?.child(node == pivot.parent?.left)?.let { direction ->
      pivot.parent?.setLeft(pivot)
    }
  }

  /**
   *
   */
  private fun findNodeByKey(key: Key): TreeNode<Key, Value>? {
    // if there is no root on the tree (it's empty), there's nothing to find
    if (root == null) return null

    // find the node with the given key
    var node = root
    while (node != null) {
      when (val comparison = key.compareTo(node.key)) {
        0 -> return node
        else -> if (key == node.key) return node else {
          node = if (comparison < 0) node.left else node.right
        }
      }
    }
    return null
  }

  /**
   *
   */
  internal fun findNodeByValue(value: Value): TreeNode<Key, Value>? {
    // locate a node by matching a value
    val stack = mutableListOf<TreeNode<Key, Value>>()
    var node = root
    while (node != null || stack.isNotEmpty()) {
      while (node != null) {
        stack.add(node)
        node = node.left
      }
      node = stack.removeAt(stack.lastIndex)
      if (value == node.value) return node
      node = node.right
    }
    return null
  }

  /**
   * Remove a node by [key].
   *
   * @param key Key for the node to remove.
   * @return The removed node, if any.
   */
  internal fun removeNodeByKey(key: Key): TreeNode<Key, Value>? {
    // if there is no root on the tree (it's empty), there's nothing to remove
    val tip = root ?: return null

    // if the node is the root node, we need to un-mount the root to remove it
    if (tip.key == key) return removeNode(tip)

    // find the node with the given key
    return findNodeByKey(key)?.let {
      removeNode(it)
    }
  }

  /**
   *
   */
  internal fun removeNodeByValue(value: Value): TreeNode<Key, Value>? {
    // locate a node by matching a value
    val stack = mutableListOf<TreeNode<Key, Value>>()
    var node = root
    while (node != null || stack.isNotEmpty()) {
      while (node != null) {
        stack.add(node)
        node = node.left
      }
      node = stack.removeAt(stack.lastIndex)
      if (value == node.value) return removeNode(node)
      node = node.right
    }
    return null
  }

  /**
   *
   */
  private fun removeNode(node: TreeNode<Key, Value>, replace: Boolean = false): TreeNode<Key, Value> = withRoot {
    // if the node has no children, remove it and return
    if (node.left == null && node.right == null) {
      (node == node.parent?.left).let { direction ->
        if (direction) {
          node.parent?.setRight(null)
        } else {
          node.parent?.setLeft(null)
        }
      }

      // node has no children and is root = evict the root, empty the map entirely
      if (root?.key == node.key && !replace) {
        evictRoot()
      } else if (!replace) {
        innerSize--
      }
      return@withRoot node
    }
    when (val parent = node.parent) {
      null -> {
        val child = node.right ?: node.left
        child?.parent = null
        root = child
      }
      else -> when {
        // if the node has one child, replace it with the child and return
        node.left == null || node.right == null -> {
          val (child, direction) = listOf(
            node.left to false,
            node.right to true,
          ).first {
            it.first != null
          }
          when (direction) {
            true -> parent.setRight(child)
            false -> parent.setLeft(child)
          }
          innerSize--
          return@withRoot node
        }

        // if the node has two children, replace it with the in-order predecessor or successor and return
        else -> {
          val successor = node.right?.let { right ->
            var current = right
            while (current.left != null) current = current.left!!
            current
          }
          successor?.let { replacement ->
            node.key = replacement.key
            node.value = replacement.value
            innerSize--
            removeNode(replacement, replace = true)
          }
        }
      }
    }
    return@withRoot node
  }

  /**
   *
   */
  protected fun evictRoot() {
    // walk the root, clearing all left/right references
    fun walkTree(node: TreeNode<Key, Value>?) {
      if (node == null) return
      walkTree(node.left)
      walkTree(node.right)
      node.left = null
      node.right = null
    }
    walkTree(root)
    root = null
    innerSize = 0u
  }

  override fun containsKey(key: Key): Boolean = findNodeByKey(key) != null
  override fun containsValue(value: Value): Boolean = findNodeByValue(value) != null
  override operator fun get(key: Key): Value? = findNodeByKey(key)?.value

  override val size: Int get() = innerSize.toInt()
  override fun isEmpty(): Boolean = root == null

  override val entries: MutableSet<
    MutableMap.MutableEntry<Key, Value>
  > get() = mutableSetOf<MutableMap.MutableEntry<Key, Value>>().apply {
    val stack = mutableListOf<TreeNode<Key, Value>>()
    var node = root
    while (node != null || stack.isNotEmpty()) {
      while (node != null) {
        stack.add(node)
        node = node.left
      }
      node = stack.removeAt(stack.lastIndex)
      add(Entry.mutable(node))
      node = node.right
    }
  }

  override val keys: MutableSet<Key> get() = mutableSetOf<Key>().apply {
    val stack = mutableListOf<TreeNode<Key, Value>>()
    var node = root
    while (node != null || stack.isNotEmpty()) {
      while (node != null) {
        stack.add(node)
        node = node.left
      }
      node = stack.removeAt(stack.lastIndex)
      add(node.key)
      node = node.right
    }
  }

  override val values: MutableCollection<Value> get() = mutableSetOf<Value>().apply {
    val stack = mutableListOf<TreeNode<Key, Value>>()
    var node = root
    while (node != null || stack.isNotEmpty()) {
      while (node != null) {
        stack.add(node)
        node = node.left
      }
      node = stack.removeAt(stack.lastIndex)
      add(node.value)
      node = node.right
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Map<*, *>) return false
    if (size != other.size) return false
    return entries.zip(other.entries).all { (a, b) -> a == b }
  }

  override fun hashCode(): Int {
    var result = root?.hashCode() ?: 0
    result = 31 * result + innerSize.toInt()
    result = 31 * result + entries.sumOf { it.hashCode() }
    return result
  }
}
