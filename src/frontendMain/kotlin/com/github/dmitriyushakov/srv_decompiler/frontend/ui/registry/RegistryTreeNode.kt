package com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry

import com.github.dmitriyushakov.srv_decompiler.frontend.model.ItemType
import com.github.dmitriyushakov.srv_decompiler.frontend.model.ListPackageResponse
import com.github.dmitriyushakov.srv_decompiler.frontend.model.Path
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.pathToString

class RegistryTreeNode(
    val key: String,
    val title: String,
    val itemType: ItemType,
    val isLeaf: Boolean,
    val path: Path,
    val sourcePathList: List<String>,
    val children: List<RegistryTreeNode>? = null
) {
    fun findByKey(key: String): RegistryTreeNode? {
        if (key == this.key) {
            return this
        } else if (children != null) {
            for (node in children) {
                val foundNode = node.findByKey(key)
                if (foundNode != null) return foundNode
            }
        }
        return null
    }

    fun replaceChildren(children: List<RegistryTreeNode>?): RegistryTreeNode {
        return RegistryTreeNode(
            key = this.key,
            title = this.title,
            itemType = this.itemType,
            isLeaf = this.isLeaf,
            path = this.path,
            sourcePathList = this.sourcePathList,
            children = children
        )
    }

    private fun transformAtKeyOrNull(key: String, transformation: (RegistryTreeNode) -> RegistryTreeNode): RegistryTreeNode? {
        if (key == this.key) {
            return transformation(this)
        } else if (children != null) {
            val newChildren: MutableList<RegistryTreeNode> = mutableListOf()
            var anyModified = false

            for (child in children) {
                val newChild = child.transformAtKeyOrNull(key, transformation)
                if (newChild == null) newChildren.add(child)
                else {
                    newChildren.add(newChild)
                    anyModified = true
                }
            }

            if (anyModified) {
                return replaceChildren(newChildren)
            } else return null
        } else return null
    }

    fun transformAtKey(key: String, transformation: (RegistryTreeNode) -> RegistryTreeNode): RegistryTreeNode {
        return transformAtKeyOrNull(key, transformation) ?: this
    }

    fun replaceChildrenAtKey(key: String, children: List<RegistryTreeNode>?): RegistryTreeNode =
        transformAtKey(key) { it.replaceChildren(children) }
}

fun List<RegistryTreeNode>.transformAtKey(key: String, transformation: (RegistryTreeNode) -> RegistryTreeNode): List<RegistryTreeNode> =
    map { it.transformAtKey(key, transformation) }

fun List<RegistryTreeNode>.replaceChildrenAtKey(key: String, children: List<RegistryTreeNode>?): List<RegistryTreeNode> =
    map { it.replaceChildrenAtKey(key, children) }

fun List<RegistryTreeNode>.findByKey(key: String): RegistryTreeNode? = firstNotNullOfOrNull { it.findByKey(key) }

fun ListPackageResponse.Item.toRegistryTreeNode(nodePathPrefix: Path): RegistryTreeNode {
    val path: Path = nodePathPrefix + listOf(name)
    return RegistryTreeNode(
        key = pathToString(path),
        title = name,
        itemType = itemType,
        isLeaf = !haveItemsInside,
        sourcePathList = sourcePathList,
        path = path
    )
}

fun ListPackageResponse.toRegistryTreeNodes(nodePathPrefix: Path): List<RegistryTreeNode> = items.map { it.toRegistryTreeNode(nodePathPrefix) }