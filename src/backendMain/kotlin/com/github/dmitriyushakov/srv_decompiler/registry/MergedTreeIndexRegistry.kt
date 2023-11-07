package com.github.dmitriyushakov.srv_decompiler.registry

import com.github.dmitriyushakov.srv_decompiler.indexer.model.Dependency
import com.github.dmitriyushakov.srv_decompiler.indexer.model.Subject

class MergedTreeIndexRegistry: MergedMapTreePathIndex<MergedTreeIndexRegistry.TreeNode>(), IndexRegistry {
    override val nodeCreator: () -> TreeNode = TreeNodeCreator

    class TreeNode(creator: () -> TreeNode): AbstractTreeNode<TreeNode>(creator) {
        var subjects: Array<Subject?>? = null
        var subjectsExists = false

        var outgoingDependencies: Array<Dependency?>? = null
        var outgoingDependenciesExists = false

        var incomingDependencies: Array<Dependency?>? = null
        var incomingDependenciesExists = false
    }

    override val subjectsIndex: PathIndex<Subject> = ParticularPathIndex(rootNode, TreeNode::subjects, TreeNode::subjectsExists, ::arrayOfNulls)
    override val outgoingDependenciesIndex: PathIndex<Dependency> = ParticularPathIndex(rootNode, TreeNode::outgoingDependencies, TreeNode::outgoingDependenciesExists, ::arrayOfNulls)
    override val incomingDependenciesIndex: PathIndex<Dependency> = ParticularPathIndex(rootNode, TreeNode::incomingDependencies, TreeNode::incomingDependenciesExists, ::arrayOfNulls)
    override fun commit() {}

    private object TreeNodeCreator: () -> TreeNode {
        override fun invoke() = TreeNode(this)
    }
}