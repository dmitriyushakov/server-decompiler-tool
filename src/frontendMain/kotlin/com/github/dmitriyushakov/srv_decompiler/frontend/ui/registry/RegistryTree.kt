package com.github.dmitriyushakov.srv_decompiler.frontend.ui.registry

import com.github.dmitriyushakov.srv_decompiler.frontend.api.API
import com.github.dmitriyushakov.srv_decompiler.frontend.component.common.reactFontIcon
import com.github.dmitriyushakov.srv_decompiler.frontend.component.tree.Tree
import com.github.dmitriyushakov.srv_decompiler.frontend.component.tree.TreeNode
import com.github.dmitriyushakov.srv_decompiler.frontend.model.ItemType
import com.github.dmitriyushakov.srv_decompiler.frontend.utils.runPromise
import io.kvision.core.Container
import io.kvision.panel.SimplePanel
import io.kvision.react.reactBind
import io.kvision.state.ObservableValue
import react.ChildrenBuilder

private fun ItemType.toFAIconClasses(): String = when(this) {
    ItemType.Package -> "fa-solid fa-box"
    ItemType.Class -> "fa-solid fa-file"
    ItemType.Field -> "fa-solid fa-hashtag"
    ItemType.Method -> "fa-solid fa-bolt"
    ItemType.LocalVar -> "fa-solid fa-hashtag"
}

class RegistryTree: SimplePanel("registry-tree") {
    val treeData: ObservableValue<List<RegistryTreeNode>> = ObservableValue(listOf())
    var onSelect: ((SelectRegistryItemEvent) -> Unit)? = null

    private fun ChildrenBuilder.renderTreeNodes(nodesData: List<RegistryTreeNode>) {
        for (node in nodesData) {
            TreeNode {
                key = node.key
                title = node.title
                isLeaf = node.isLeaf
                icon = reactFontIcon(node.itemType.toFAIconClasses())

                val children = node.children
                if (children != null) renderTreeNodes(children)
            }
        }
    }

    init {
        reactBind(treeData) { accessTree, modifyTree ->
            val nodesData = accessTree()

            Tree {
                loadData = { node ->
                    runPromise {
                        val nodesData = accessTree()
                        val eventKey: String = node.props.eventKey
                        val treeNode = nodesData.findByKey(eventKey)
                        if (treeNode != null) {
                            val response = API.IndexRegistry.listPackage(treeNode.path)

                            val newChildren = response.toRegistryTreeNodes(treeNode.path)
                            val newNodesData = nodesData.replaceChildrenAtKey(eventKey, newChildren)
                            this@RegistryTree.treeData.setState(newNodesData)
                        }
                    }
                }

                onSelect = { keys, event ->
                    for (key in keys) {
                        val treeNode = nodesData.findByKey(key)
                        if (treeNode != null) {
                            this@RegistryTree.onSelect?.let { handler ->
                                val event = SelectRegistryItemEvent(
                                    path = treeNode.path,
                                    sourcePathList = treeNode.sourcePathList,
                                    itemType = treeNode.itemType
                                )
                                handler(event)
                            }
                            break
                        }
                    }
                }

                renderTreeNodes(nodesData)
            }
        }

        addAfterInsertHook {
            runPromise {
                val response = API.IndexRegistry.listPackage(listOf("."))
                treeData.setState(response.toRegistryTreeNodes(emptyList()))
            }
        }
    }
}

fun Container.registryTree(init: (RegistryTree.() -> Unit)? = null): RegistryTree {
    val tree = RegistryTree()
    init?.let { init ->
        tree.init()
    }
    add(tree)
    return tree
}