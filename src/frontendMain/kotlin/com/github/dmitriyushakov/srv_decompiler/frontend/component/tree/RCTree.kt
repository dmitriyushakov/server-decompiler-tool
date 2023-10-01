package com.github.dmitriyushakov.srv_decompiler.frontend.component.tree

import io.kvision.require
import react.ComponentClass
import react.PropsWithChildren
import kotlin.js.Promise

external interface TreeProps: PropsWithChildren {
    var autoExpandParent: Boolean
    var checkable: Boolean
    var checkStrictly: Boolean
    var className: String
    var defaultCheckedKeys: Array<String>
    var defaultExpandedKeys: Array<String>
    var defaultExpandAll: Boolean
    var defaultExpandParent: Boolean
    var defaultSelectedKeys: Array<String>
    var disabled: Boolean
    var expandedKeys: Array<String>
    var icon: dynamic
    var loadedKeys: Array<String>
    var loadData: (dynamic) -> Promise<Unit>
    var showIcon: Boolean
    var showLine: Boolean
    var treeData: Array<dynamic>
    var onSelect: (Array<String>, dynamic) -> Unit
}

val Tree: ComponentClass<TreeProps> = require("rc-tree").default

external interface TreeNodeProps: PropsWithChildren {
    var className: String
    var checkable: Boolean
    var disabled: Boolean
    var disableCheckbox: Boolean
    var title: dynamic
    var isLeaf: Boolean
    var icon: dynamic
}

val TreeNode: ComponentClass<TreeNodeProps> = require("rc-tree").TreeNode